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

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManagerFactory;
import javax.servlet.ServletContext;

public class App {
    public static PersistenceManagerFactory getPMF(ServletContext ctx) {
        PersistenceManagerFactory pmfFactory =
            (PersistenceManagerFactory) ctx.getAttribute(
                    PersistenceManagerFactory.class.getName());
        if (pmfFactory == null) {
            pmfFactory = JDOHelper
                .getPersistenceManagerFactory("transactions-optional");
            ctx.setAttribute(
                    PersistenceManagerFactory.class.getName(),
                    pmfFactory);
        }
        return pmfFactory;
    }
}
