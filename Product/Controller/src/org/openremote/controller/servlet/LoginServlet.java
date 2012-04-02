/*
 * OpenRemote, the Home of the Digital Home.
 * Copyright 2008-2012, OpenRemote Inc.
 *
 * See the contributors.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.controller.servlet;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.File;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.Logger;
import org.openremote.controller.Constants;
import org.openremote.controller.ControllerConfiguration;
import org.openremote.controller.service.ConfigurationService;
import org.openremote.controller.spring.SpringContext;
import org.openremote.controller.utils.PathUtil;
import org.springframework.security.providers.encoding.Md5PasswordEncoder;

/**
 * This servlet is used to authenticate an administrator and set a session if successful
 * 
 * @author <a href="mailto:vincent.kriek@tass.nl">Vincent Kriek</a>
 * 
 */
public class LoginServlet extends HttpServlet
{

/**
   * Common log category for HTTP
   */
  private final static Logger logger = Logger.getLogger(Constants.LOGIN_SERVLET_LOG_CATEGORY);
  
  private ConfigurationService configurationService = (ConfigurationService) SpringContext
        .getInstance().getBean("configurationService");
  
  private ControllerConfiguration configuration = ControllerConfiguration.readXML();
  

  /**
   * Check username and password against the online interface
   * @param username The OpenRemote Composer username
   * @param password The OpenRemote Composer password
   * @return If the combo is valid
   */
  public boolean checkOnline(String username, String password)
  {

     if(!username.equals(configurationService.getItem("composer_username"))) {
        return false;
     }

     HttpClient httpClient = new DefaultHttpClient();
     HttpGet httpGet = new HttpGet(PathUtil.addSlashSuffix(configuration.getBeehiveRESTRootUrl()) + "user/" + username
           + "/openremote.zip");

     httpGet.setHeader(Constants.HTTP_AUTHORIZATION_HEADER, Constants.HTTP_BASIC_AUTHORIZATION
           + encode(username, password));

     try {
        HttpResponse resp= httpClient.execute(httpGet);
        if (200 == resp.getStatusLine().getStatusCode()) {
           return true;
        } 
        
     } catch (IOException e) {
        return false;
     }
     return false;
  }
  
  /**
   * Encode username and password for sending to the Beehive rest controller
   * @param username The OpenRemote Composer username
   * @param password The OpenRemote Composer password
   * @return String to send to the Beehive rest model
   */
  private String encode(String username, String password) {
     Md5PasswordEncoder encoder = new Md5PasswordEncoder();
     String encodedPwd = encoder.encodePassword(password, username);
     if (username == null || encodedPwd == null) {
        return null;
     }
     return new String(Base64.encodeBase64((username + ":" + encodedPwd).getBytes()));
  }
  
  /**
   * Handle login request, redirect to administrator if successful, show login age if not
   * @param request the request from HTTP servlet
   * @param reponse where the respond can be written to
   */  
  @Override 
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
                                                              throws ServletException, IOException
  {
     String username = request.getParameter("username");
     String password = request.getParameter("password");
     HttpSession session = request.getSession(true);
     PrintWriter out = response.getWriter();
     Object auth = session.getAttribute("authenticated");
     
     logger.error("username: " + username + "password: " + password);
     
     if(auth != null) {
        out.write(auth.toString());
     }
     
     if(checkOnline(username, password)) {
        session.setAttribute("authenticated", true);
        
        response.sendRedirect("/controller/administrator");
     } else {
        doGet(request, response);
     }
  }
  
  /**
   * Return the login page
   * @param request the request from HTTP servlet
   * @param reponse where the respond can be written to
   */  
  @Override 
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
                                                              throws ServletException, IOException
  {
     PrintWriter out = response.getWriter();
     
     File file = new File(configuration.getResourcePath() + "/login.html");
     BufferedReader read = new BufferedReader(new FileReader(file));
     String line = null;
     while((line = read.readLine()) != null) 
     {
        out.write(line);
     }
  }
}
