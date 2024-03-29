/* OpenRemote, the Home of the Digital Home.
* Copyright 2008-2011, OpenRemote Inc.
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
package org.openremote.controller.component;

import java.util.ArrayList;
import java.util.List;

import org.openremote.controller.command.NoStatusCommand;
import org.openremote.controller.command.StatusCommand;

/**
 * Super class of all components
 * 
 * @author Handy.Wang 2009-12-31
 */
public abstract class Component {
   
   private Sensor sensor;
   
   /** The Constant REF_ATTRIBUTE_NAME. */
   public static final String REF_ATTRIBUTE_NAME = "ref";
   
   public static final String INCLUDE_ELEMENT_NAME = "include";
   
   public static final String INCLUDE_TYPE_ATTRIBUTE_NAME = "type";
   
   public static final String COMMAND_ELEMENT_NAME= "command";
   
   public static final String STATUS_ELEMENT_NAME = "status";
   
   public static final String INCLUDE_TYPE_SENSOR = "sensor";
   
   public static final String INCLUDE_TYPE_GROUP = "group";
   
   protected List<String> availableActions;
   
   /**
    * Instantiates a new Component.
    */
   public Component() {
       super();
       setSensor(new Sensor(new NoStatusCommand()));
       availableActions = new ArrayList<String>();
       availableActions.addAll(getAvailableActions());
   }
   
   /** All available actions of sub controls */
   protected abstract List<String> getAvailableActions();
   
   public boolean isValidActionWith(String actionParam) {
      for (String action : availableActions) {
         if (action.equalsIgnoreCase(actionParam)) {
            return true;
         }
      }
      return false;
   }
   
   /**
    * Gets the status command.
    * 
    * @return the status command
    */
   public StatusCommand getStatusCommand() {
       return sensor.getStatusCommand();
   }

   protected Sensor getSensor() {
      return sensor;
   }


   public void setSensor(Sensor sensor) {
      this.sensor = sensor;
   }
}
