/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.seaborne.delta.server2.http;

import java.io.IOException ;
import java.io.InputStream ;

import javax.servlet.http.HttpServletRequest ;
import javax.servlet.http.HttpServletResponse ;

import org.apache.jena.web.HttpSC ;
import org.seaborne.delta.Delta ;
import org.seaborne.delta.server2.API ;
import org.seaborne.delta.server2.Id ;
import org.slf4j.Logger ;

/** Receive an incoming patch.
 * Translates from HTTP to the internal protocol agnostic API. 
 */
public class S_Patch extends ServletBase {
    static private Logger LOG = Delta.getDeltaLogger("Patch") ;
    
    public S_Patch() {}
    
    @Override
    protected void doPatch(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        doPost(req, resp);
    }
    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        LOG.info("Patch");
        Id ref ; 
        try {
            ref = getDataId(req) ;
            LOG.info("Dataset ref = "+ref);
        } catch (RuntimeException ex) {
            resp.sendError(HttpSC.BAD_REQUEST_400, "Bad data id") ;
            return ;
        }
        try {
            try (InputStream in = req.getInputStream()) {
                API.receive(ref, in) ;
            }
            resp.setStatus(HttpSC.NO_CONTENT_204) ;
        } catch (RuntimeException ex) {
            LOG.warn("Failed to process", ex); 
            resp.sendError(HttpSC.INTERNAL_SERVER_ERROR_500, ex.getMessage()) ;
            return ;
        }
    }
    
    public static String paramClient = "client" ;
    public static String paramDataset = "dataset" ;
    
    // Either a path name /patch/ 
    private static Id getDataId(HttpServletRequest req) {
        String datasetStr = req.getParameter(paramDataset) ;
        if ( datasetStr == null ) {
            String s = req.getServletPath() ;
            String requestURI = req.getRequestURI() ;
            if ( requestURI.startsWith(s) ) {
                String dname = requestURI.substring(s.length()) ;
                LOG.info("Dataset name = "+dname);
                return Id.fromString(dname) ;
            }
        }
        
        
        
        //String clientStr = req.getParameter(paramClient) ;
        
        Id uuid ;
        try {
             return  Id.fromString(datasetStr) ;
        } catch (IllegalArgumentException ex) {
            throw ex ;
        }
    }
}
