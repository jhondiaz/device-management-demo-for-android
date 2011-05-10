<%
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
%>

<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.io.IOException" %>
<%@ page import="javax.jdo.JDOObjectNotFoundException" %>
<%@ page import="javax.jdo.PersistenceManager" %>
<%@ page import="com.google.appengine.api.datastore.Key" %>
<%@ page import="com.google.appengine.api.datastore.KeyFactory" %>
<%@ page import="com.google.appengine.api.users.User" %>
<%@ page import="com.google.appengine.api.users.UserService" %>
<%@ page import="com.google.appengine.api.users.UserServiceFactory" %>
<%@ page import="com.admsample.server.*" %>

<html>
  <head>
  <link  href="http://fonts.googleapis.com/css?family=Droid+Sans:regular" rel="stylesheet" type="text/css" />
  <link  href="/main.css" rel="stylesheet" type="text/css" />
  <%
    UserService userService = UserServiceFactory.getUserService();
    User user = userService.getCurrentUser();
    
    if (user == null) {
        response.sendRedirect(userService.createLoginURL(request.getRequestURI()));
        return;
    }
    
    final boolean isPost = "POST".equals(request.getMethod());
    String updateMessage = "";
    String passwordType = null;
    int passwordLength = -1;
    Policy policy = null;
    boolean foundError = false;
    PersistenceManager pm =
        App.getPMF(getServletContext()).getPersistenceManager();
    if (isPost) {
        
        try {
            passwordLength = Integer.valueOf(request.getParameter(Policy.PASSWORD_LENGTH)).intValue();
            if (passwordLength < 1) {
                updateMessage = "Password length must be at least 1.";
                foundError = true;
            }
        } catch (NumberFormatException exception) {
            updateMessage = "Invaid password length.";
            foundError = true;
        }
        
        passwordType = request.getParameter(Policy.PASSWORD_TYPE);
        if (passwordType == null || passwordType.trim().length() == 0) {
            updateMessage = "Missing password type.";
            foundError = true;
        }
        
        if (!foundError) {
            Key key = KeyFactory.createKey(Policy.class.getSimpleName(), user.getEmail());
            policy = new Policy(key, passwordLength, passwordType, true);
            try {
                pm.makePersistent(policy);
                updateMessage = "Policy saved successfully.";
            } catch (Exception e) {
                response.setStatus(500);
                e.printStackTrace();
                pm.close();
            }
        }
    }
    // Retrieves saved policy.
    try {
        Key key = KeyFactory.createKey(Policy.class.getSimpleName(), user.getEmail());
        try {
            policy = pm.getObjectById(Policy.class, key);
            if (policy != null) {
                passwordType = policy.getPasswordType();
                passwordLength = policy.getPasswordLength();
            }
        } catch (JDOObjectNotFoundException e) {
            updateMessage = "Policy not found.";
        }
    } finally {
        pm.close();
    }
%>
    <title>Device Management Demo for Android - Security Policy</title>
  </head>
  <body>
<div id="container">
  <div id="top">
    <div id="loginLinks">
      Hello, <%= user.getNickname() %> [<a href="<%=UserServiceFactory.getUserService().createLogoutURL("/policy.jsp")%>">logout</a>]
    </div>
    <div>
      <b>Policy Setup</b> | <a href="admin.jsp">Remote Admin</a>
    </div>  
  </div>
  <div id="updateMessage"><%=updateMessage%></div>
  <div id="policyForm">
    <form action="/policy.jsp" method="POST">
      <table align="center" cellpadding="4" cellspacing="0">
        <tr>
          <td class="descCol">
            <div class="desc" align="left">Password Type:</div>
          </td>
          <td>
            <div align="left">
              <select name="passwordType" class="pwtype">
                <option value="">-Choose One-</option>
                <option value="ALPHA" <%="ALPHA".equals(passwordType) ? "selected" : ""%>>ALPHA</option> 
                <option value="PIN" <%="PIN".equals(passwordType) ? "selected" : ""%>>PIN</option> 
              </select>        
            </div>
          </td>
        </tr>
        <tr>
          <td class="descCol">
            <div class="desc" align="left">Password Length:</div>
          </td>    
          <td>
            <div align="left">
              <input class="input" type="text" name="passwordLength" value="<%=passwordLength%>" />
            </div>
          </td>
        </tr>
        <tr>
          <td colspan="2">
            <div align="right">
              <input type="submit" value="Save policy" />
            </div>  
          </td>
        </tr>
      </table>   
    </form>
  </div>
</div>
</body>
</html>