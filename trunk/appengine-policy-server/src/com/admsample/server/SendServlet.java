/*
 * Copyright 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.admsample.server;

import java.io.IOException;
import java.util.logging.Logger;

import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.android.c2dm.server.C2DMessaging;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserServiceFactory;

@SuppressWarnings("serial")
public class SendServlet extends HttpServlet {
    private static final Logger log =
        Logger.getLogger(SendServlet.class.getName());
    private static final String OK_STATUS = "OK";
    private static final String DEVICE_NOT_REGISTERED_STATUS = "DEVICE_NOT_REGISTERED";
    private static final String ERROR_STATUS = "ERROR";

    @Deprecated
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        doPost(req, resp);
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/plain");

        String pushCommand = req.getParameter("command");
        User user = null;
        if (pushCommand != null) {
            user = RegisterServlet.checkUser(req, resp, false);
            if (user == null) {
                resp.sendRedirect(UserServiceFactory.getUserService().createLoginURL(req.getRequestURI()));
                return;
            }
        }
        if ("lock".equals(pushCommand) || "syncPolicy".equals(pushCommand)) {
            sendCommand(user.getEmail(), resp, pushCommand);
        } else {
            resp.getWriter().println("Nothing to push");
        }
    }

    private boolean sendCommand(String userAccount, HttpServletResponse resp, final String command)
        throws IOException {
        // Get device info
        DeviceInfo deviceInfo = null;
        // Shared PMF
        PersistenceManager pm =
                C2DMessaging.getPMF(getServletContext()).getPersistenceManager();
        try {
            Key key = KeyFactory.createKey(DeviceInfo.class.getSimpleName(), userAccount);
            try {
                deviceInfo = pm.getObjectById(DeviceInfo.class, key);
            } catch (JDOObjectNotFoundException e) {
                log.warning("Device not registered");
                resp.getWriter().println(DEVICE_NOT_REGISTERED_STATUS);
                return false;
            }
        } finally {
            pm.close();
        }

        // Send push message to device
        C2DMessaging push = C2DMessaging.get(getServletContext());
        boolean res = false;
        String collapseKey = "" + System.currentTimeMillis();
        res = push.sendNoRetry(deviceInfo.getDeviceRegistrationID(),
                    collapseKey,
                    "command", command);
        
        if (res) {
            log.info("Command sent to device! collapse_key:" + collapseKey);
            resp.getWriter().println(OK_STATUS);
            return true;
        } else {
            log.warning("Error: Unable to send command to device.");
            resp.setStatus(500);
            resp.getWriter().println(ERROR_STATUS + " (Unable to send command)");
            return false;
        }
    }
}
