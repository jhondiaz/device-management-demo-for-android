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
  <script type="text/javascript">
     function sendPush(pushCommand) {
       var params = 'command=' + pushCommand;
       var http = new XMLHttpRequest();
       http.open('POST', '/send', true);
       http.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
       http.setRequestHeader("Content-length", params.length);
       http.setRequestHeader("Connection", "close");
       http.onreadystatechange = function() {
         if (http.readyState == 4 && http.status == 200) {
           document.getElementById("pushMessage").innerHTML = 'Push status: ' + http.responseText;
         }
       }
       http.send(params);
       return false;
     }
  </script>
<%
    UserService userService = UserServiceFactory.getUserService();
    User user = userService.getCurrentUser();
    
    if (user == null) {
        response.sendRedirect(userService.createLoginURL(request.getRequestURI()));
        return;
    }
    
    String policyMessage = "";
    String passwordType = null;
    int passwordLength = -1;
    Policy policy = null;
    PersistenceManager pm =
        App.getPMF(getServletContext()).getPersistenceManager();

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
            policyMessage = "Policy not found.";
        }
    } finally {
        pm.close();
    }
%>
    <title>Device Management Demo for Android - Remote Admin</title>
  </head>
  <body>
    <div id="container">
      <div id="top">
        <div id="loginLinks">
          Hello, <%= user.getNickname() %> [<a href="<%=UserServiceFactory.getUserService().createLogoutURL("/policy.jsp")%>">logout</a>]
        </div>
        <div>
        <a href="policy.jsp">Policy Setup</a> | <b>Remote Admin</b>
        </div>  
      </div>
<% 
    if (policy == null) {
%>    <div id="updateMessage">
        Policy not found.  Set it up <a href="/policy.jsp">here.</a>
      </div>  
<%  
    } else {
%>
      <div id="adminForm">
        <form id="push" action="/send" method="POST">
          <input type="hidden" name="command"/>
          <table align="center" cellpadding="4" cellspacing="0">
            <tr>
              <td class="descCol">
                <div class="desc" align="left">Password Type:</div>
              </td>
              <td>
                <div class="desc" align="left"><%=passwordType%></div>
              </td>
            </tr>
            <tr>
              <td class="descCol">
                <div class="desc" align="left">Password Length:</div>
              </td>
              <td>
                <div class="desc" align="left"><%=passwordLength%></div>
              </td>
            </tr>
            <tr>
              <td colspan="2">
                <input type="button" value="Lock Device" onclick="sendPush('lock');" />
                <input type="button" value="Send Policy" onclick="sendPush('syncPolicy');" />
              </td>
            </tr>
          </table>
        </form>
      </div>
      <div id="pushMessage"></div>
<%  } 
%>
    </div> <!-- container -->
  </body>
</html>