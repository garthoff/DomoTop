/* OpenRemote, the Home of the Digital Home.
* Copyright 2008-2010, OpenRemote Inc.
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

package org.openremote.android.console.util;

import java.util.Iterator;
import java.io.IOException;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLProtocolException;

import org.apache.http.HttpResponse;
import org.openremote.android.console.AppSettingsActivity;
import org.openremote.android.console.Constants;
import org.openremote.android.console.GroupActivity;
import org.openremote.android.console.LoginViewActivity;
import org.openremote.android.console.Main;
import org.openremote.android.console.R;
import org.openremote.android.console.model.AppSettingsModel;
import org.openremote.android.console.model.ControllerException;
import org.openremote.android.console.model.ViewHelper;
import org.openremote.android.console.model.XMLEntityDataBase;
import org.openremote.android.console.net.ORControllerServerSwitcher;
import org.openremote.android.console.net.ORNetworkCheck;
import org.openremote.android.console.ssl.ORKeyStore;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.TextUtils.TruncateAt;
import android.util.Log;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * It's responsible for downloading resources in backgroud and updte progress in text.
 * 
 * @author handy 2010-05-10
 * @author Dan Cong
 *
 */
public class AsyncResourceLoader extends AsyncTask<Void, String, AsyncResourceLoaderResult> {
   private static final int TO_LOGIN = 0xF00A;
   private static final int TO_SETTING_SSL_PROTOCOL_ERROR = 0xF00B;
   private static final int TO_SETTING_SSL_ERROR = 0xF00C;
   private static final int TO_GROUP = 0xF00D;
   private static final int SWITCH_TO_OTHER_CONTROLER = 0xF00E;
   
   public final static String LOG_CATEGORY = Constants.LOG_CATEGORY + AsyncResourceLoader.class.getName();
   private ORKeyStore orKeyStore;
   
   private Activity activity;
   private AsyncGroupLoader groupLoader;	
   
   public AsyncResourceLoader(Activity activity) {
      this.activity = activity;
      
      /**
       * Start the retrieving our group from the ORB
       */
      this.groupLoader = new AsyncGroupLoader(activity.getApplicationContext());
      this.groupLoader.start();
      this.orKeyStore = ORKeyStore.getInstance(activity);
   }
   
   /** 
    * Download resources in background.
    * 
    * @see android.os.AsyncTask#doInBackground(Params[])
    */
   @Override
   protected AsyncResourceLoaderResult doInBackground(Void... params) {
      AsyncResourceLoaderResult result = new AsyncResourceLoaderResult();
      boolean isDownloadSuccess = false;
      String panelName = AppSettingsModel.getCurrentPanelIdentity(activity);
      publishProgress("panel: " + panelName);

      Log.i("OpenRemote/DOWNLOAD", "Getting panel: " + panelName);

      String serverUrl = AppSettingsModel.getSecuredServer(activity);

      HttpResponse checkResponse = null;

      try
      {
        checkResponse = ORNetworkCheck.verifyControllerURL(activity, AppSettingsModel.getCurrentServer(activity));
      }
      catch (SSLProtocolException e) 
      {
		//Probably no access
		result.setAction(TO_SETTING_SSL_PROTOCOL_ERROR);
		Log.e(LOG_CATEGORY, "AsyncReSourceLoader: ", e);  
		
		// Delete & create a new keystore
		orKeyStore.delete();
		orKeyStore.create();
  		return result;	  
      }
      catch (SSLException e)
      {
  		//Probably no access
  		result.setAction(TO_SETTING_SSL_ERROR);
    	Log.e(LOG_CATEGORY, "AsyncReSourceLoader: ", e);
  		return result;
      }
      catch (IOException e)
      {
        // TODO :
        //   right now still not doing anything with this, user is still completely in the dark
        //   what went wrong, need to untangle the async loader mess before can propagate info
        //   back to user
        //
        //   So for now, an empty catch block, until I can build proper test cases for these
        //                                                                                    [JPL]
        //
    	if(e != null) {
	    	Log.e(LOG_CATEGORY, "AsyncReSourceLoader: ", e);
    		Log.e("OpenRemote/DOWNLOAD", e.getMessage());
	    }
      }

     
      isDownloadSuccess = checkResponse != null && checkResponse.getStatusLine().getStatusCode() == Constants.HTTP_SUCCESS;

      if (isDownloadSuccess) {
         int downLoadPanelXMLStatusCode = HTTPUtil.downLoadPanelXml(activity, serverUrl, panelName);
         if (downLoadPanelXMLStatusCode != Constants.HTTP_SUCCESS) { // download panel xml fail.
            Log.i("OpenRemote/DOWNLOAD", "Download file panel.xml fail.");
            if (downLoadPanelXMLStatusCode == ControllerException.UNAUTHORIZED) {
             result.setAction(TO_LOGIN);
             result.setStatusCode(downLoadPanelXMLStatusCode);
             return result;
            }
            
            if (activity.getFileStreamPath(Constants.PANEL_XML).exists()) {
               Log.i("OpenRemote/DOWNLOAD", "Download file panel.xml fail, so use local cache.");
               FileUtil.parsePanelXML(activity);
               result.setCanUseLocalCache(true);
               result.setStatusCode(downLoadPanelXMLStatusCode);
               result.setAction(TO_GROUP);
            } else {
               Log.i("OpenRemote/DOWNLOAD", "No local cache is available, ready to switch controller.");
               result.setAction(SWITCH_TO_OTHER_CONTROLER);
               return result;
            }
         } else { // download panel xml success.
            Log.i("OpenRemote/DOWNLOAD", "Download file panel.xml successfully.");
            if (activity.getFileStreamPath(Constants.PANEL_XML).exists()) {
               FileUtil.parsePanelXML(activity);
               result.setAction(TO_GROUP);
            } else {
               Log.i("OpenRemote/DOWNLOAD","No local cache is available authouth downloaded file panel.xml successfully, ready to switch controller.");
               result.setAction(SWITCH_TO_OTHER_CONTROLER);
               return result;
            }

            Iterator<String> images = XMLEntityDataBase.imageSet.iterator();
            String imageName = "";
            while (images.hasNext()) {
               imageName = images.next();
               publishProgress(imageName);
               HTTPUtil.downLoadImage(activity, AppSettingsModel.getSecuredServer(activity), imageName);
            }
         }
      } else { // Download failed.
         if (checkResponse != null && checkResponse.getStatusLine().getStatusCode() == ControllerException.UNAUTHORIZED) {
        	String html = "";
        	
			try {
				html = StringUtil.stringFromInputStream(checkResponse.getEntity().getContent());
			} catch (IllegalStateException e) {
				Log.e(LOG_CATEGORY, e.getMessage());
			} catch (IOException e) {
				Log.e(LOG_CATEGORY, e.getMessage());
			}
			
        	if(AppSettingsModel.isSSLEnabled(activity) || html.contains("client certificate chain")) 
        	{
        		result.setAction(TO_SETTING_SSL_ERROR);		
        	} else {
        		result.setAction(TO_LOGIN);
            	result.setStatusCode(ControllerException.UNAUTHORIZED);
        	}
            return result;
         }
         
         if(checkResponse != null && checkResponse.getStatusLine().getStatusCode() == 403) {
     		result.setAction(TO_SETTING_SSL_ERROR);
            return result;
         }
         
         if (activity.getFileStreamPath(Constants.PANEL_XML).exists()) {
            Log.i("OpenRemote/DOWNLOAD", "Download failed, so use local cache.");
            FileUtil.parsePanelXML(activity);
            result.setCanUseLocalCache(true);
            result.setAction(TO_GROUP);
            if (checkResponse == null) {
               result.setStatusCode(ControllerException.CONTROLLER_UNAVAILABLE);
            } else {
               result.setStatusCode(checkResponse.getStatusLine().getStatusCode());
            }
         } else {
            Log.i("OpenRemote/DOWNLOAD", "No local cache is available, ready to switch controller.");
            result.setAction(SWITCH_TO_OTHER_CONTROLER);
            return result;
         }
      }

      new Thread(new Runnable() {
         public void run() {
            ORControllerServerSwitcher.detectGroupMembers(activity);
         }
      }).start();

      if(result.getAction() == TO_GROUP && !groupLoader.isDone()) {
    	  synchronized(groupLoader) {
    		  try {
				groupLoader.wait();
			} catch (InterruptedException e) {
	            Log.e("OpenRemote/DOWNLOAD", "Waiting for groupLoader interrupted");
			}
    	  }
      }
      
      return result;
   }

   /**
    * Update progress message in text.
    * 
    * @see android.os.AsyncTask#onProgressUpdate(Progress[])
    */
   @Override
   protected void onProgressUpdate(String... values) {
      RelativeLayout loadingView = (RelativeLayout) (activity.findViewById(R.id.welcome_view));
      if (loadingView == null) {
         return;
      }
      
      TextView loadingText = (TextView)(activity.findViewById(R.id.loading_text));
      loadingText.setText("loading " + values[0] + "...");
      loadingText.setEllipsize(TruncateAt.MIDDLE);
      loadingText.setSingleLine(true);
   }

   /**
    * Finish downloading and forward to different view by result.
    * 
    * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
    */
   @Override
   protected void onPostExecute(AsyncResourceLoaderResult result) {
      publishProgress("groups & screens");
      Intent intent = new Intent();
      switch (result.getAction()) {
      case TO_GROUP:
         intent.setClass(activity, GroupActivity.class);
         if (result.isCanUseLocalCache()) {
            intent.setData(Uri.parse(ControllerException.exceptionMessageOfCode(result.getStatusCode())));
         }
         break;
      case TO_LOGIN:
         intent.setClass(activity, LoginViewActivity.class);
         intent.setData(Uri.parse(Main.LOAD_RESOURCE));
         break;
      case TO_SETTING_SSL_PROTOCOL_ERROR:
    	  //If the host is in the keystore, delete it. It is obviously invalid.
    	  orKeyStore.deleteHost(AppSettingsModel.getCurrentServer(activity));
    	  
    	  intent.setClass(activity, AppSettingsActivity.class);    	  
    	  intent.putExtra("SSL_CLIENT_PROTOCOL", true);
    	  break;
      case TO_SETTING_SSL_ERROR:
    	  //If the host is in the keystore, delete it. It is obviously invalid.
    	  orKeyStore.deleteHost(AppSettingsModel.getCurrentServer(activity));
    	  
    	  intent.setClass(activity, AppSettingsActivity.class);    	  
    	  intent.putExtra("SSL_CLIENT", true);
    	  break;
      case SWITCH_TO_OTHER_CONTROLER:
        
         ORControllerServerSwitcher.doSwitch(activity);
         return;

      default:
         ViewHelper.showAlertViewWithTitle(activity, "Send Request Error", ControllerException.exceptionMessageOfCode(result.getStatusCode()));
         return;
      }
      
      activity.startActivity(intent);
      activity.finish();
   }
}

/**
 * To express the downloading result state.
 */
class AsyncResourceLoaderResult {
   
   /** The action after downloading. */
   private int action;
   
   /** Download resources status code. */
   private int statusCode;
   
   /** If download failed, can use local cache or not. */
   private boolean canUseLocalCache;

   public AsyncResourceLoaderResult() {
      action = -1;
      statusCode = -1;
      canUseLocalCache = false;
   }

   public int getAction() {
      return action;
   }

   public void setAction(int action) {
      this.action = action;
   }

   public int getStatusCode() {
      return statusCode;
   }

   public void setStatusCode(int statusCode) {
      this.statusCode = statusCode;
   }

   public boolean isCanUseLocalCache() {
      return canUseLocalCache;
   }

   public void setCanUseLocalCache(boolean canUseLocalCache) {
      this.canUseLocalCache = canUseLocalCache;
   }

}
