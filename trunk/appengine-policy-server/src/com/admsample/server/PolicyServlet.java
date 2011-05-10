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
import java.io.PrintWriter;
import java.util.List;
import java.util.logging.Logger;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.c2dm.server.C2DMessaging;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;

@SuppressWarnings("serial")
public class PolicyServlet extends HttpServlet {
    private static final Logger log =
        Logger.getLogger(PolicyServlet.class.getName());    
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = checkUser();
        if (user == null) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Unable to authenticate");
            return;
        }
        PersistenceManager persistenceManager =
            C2DMessaging.getPMF(getServletContext()).getPersistenceManager();
        Query query = persistenceManager.newQuery(Policy.class);
        query.setFilter("key == ownerKeyParam");
        query.declareParameters(Key.class.getName() + " ownerKeyParam");
        Key key = KeyFactory.createKey(Policy.class.getSimpleName(), user.getEmail());
        List<Policy> policies = (List<Policy>) query.execute(key);
        
        resp.setContentType("text/plain");
        PrintWriter writer = resp.getWriter();
        if (policies != null && policies.size() > 0) {
            // The query should just return one item.
            Policy policy = policies.get(0);
            JSONObject jsonPolicy = new JSONObject();
            try {
                jsonPolicy.accumulate("policy", policy.toJSON());
            } catch (JSONException exception) {
                exception.printStackTrace();
            }
            writer.println(jsonPolicy.toString());
        } else {
            writer.println("{}");
        }
        writer.close();
    }
      
      @Override
      protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
          IOException {  
        doPost(req, resp);
        return;
      }
      
      private User checkUser() {
        UserService userService = UserServiceFactory.getUserService();
        User user = userService.getCurrentUser();
        return user;
      }
}
