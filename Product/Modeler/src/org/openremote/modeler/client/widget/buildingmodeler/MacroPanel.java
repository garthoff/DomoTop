/* OpenRemote, the Home of the Digital Home.
* Copyright 2008-2009, OpenRemote Inc.
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

package org.openremote.modeler.client.widget.buildingmodeler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openremote.modeler.client.event.DoubleClickEvent;
import org.openremote.modeler.client.event.SubmitEvent;
import org.openremote.modeler.client.gxtextends.SelectionServiceExt;
import org.openremote.modeler.client.gxtextends.SourceSelectionChangeListenerExt;
import org.openremote.modeler.client.icon.Icons;
import org.openremote.modeler.client.listener.ConfirmDeleteListener;
import org.openremote.modeler.client.listener.EditDelBtnSelectionListener;
import org.openremote.modeler.client.listener.SubmitListener;
import org.openremote.modeler.client.proxy.BeanModelDataBase;
import org.openremote.modeler.client.proxy.DeviceMacroBeanModelProxy;
import org.openremote.modeler.client.rpc.AsyncSuccessCallback;
import org.openremote.modeler.client.widget.TreePanelBuilder;
import org.openremote.modeler.domain.Device;
import org.openremote.modeler.domain.DeviceCommand;
import org.openremote.modeler.domain.DeviceCommandRef;
import org.openremote.modeler.domain.DeviceMacro;
import org.openremote.modeler.domain.DeviceMacroItem;
import org.openremote.modeler.domain.DeviceMacroRef;
import org.openremote.modeler.selenium.DebugId;

import com.extjs.gxt.ui.client.data.BeanModel;
import com.extjs.gxt.ui.client.data.ChangeEvent;
import com.extjs.gxt.ui.client.data.ChangeEventSupport;
import com.extjs.gxt.ui.client.data.ChangeListener;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.store.Store;
import com.extjs.gxt.ui.client.store.TreeStoreEvent;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.Info;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.toolbar.ToolBar;
import com.extjs.gxt.ui.client.widget.treepanel.TreePanel;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Element;

/**
 * The Class MacroPanel.
 */
public class MacroPanel extends ContentPanel {

   /** The icons. */
   private Icons icons = GWT.create(Icons.class);

   /** The macro tree. */
   private TreePanel<BeanModel> macroTree = null;

   /** The macro list container. */
   private LayoutContainer macroListContainer = null;

   /** The change listener map. */
   private Map<BeanModel, ChangeListener> changeListenerMap = null;

   /** The selection service. */
   private SelectionServiceExt<BeanModel> selectionService;
   
   /**
    * Instantiates a new macro panel.
    */
   public MacroPanel() {
      setHeading("Macros");
      setLayout(new FitLayout());
      selectionService = new SelectionServiceExt<BeanModel>();
      createMenu();
      createMacroTree();
      setIcon(icons.macroIcon());
      getHeader().ensureDebugId(DebugId.DEVICE_MACRO_PANEL_HEADER);
   }

   /**
    * Creates the menu.
    */
   private void createMenu() {
      ToolBar macroToolBar = new ToolBar();
      Button newMacroBtn = new Button("New");
      newMacroBtn.setToolTip("Create Macro");
      newMacroBtn.setIcon(icons.macroAddIcon());
      newMacroBtn.ensureDebugId(DebugId.NEW_MACRO_BTN);
      newMacroBtn.addSelectionListener(new SelectionListener<ButtonEvent>() {
         @Override
         public void componentSelected(ButtonEvent ce) {
            final MacroWindow macroWindow = new MacroWindow();

            macroWindow.addListener(SubmitEvent.SUBMIT, new SubmitListener() {
               @Override
               public void afterSubmit(SubmitEvent be) {
                  afterCreateDeviceMacro(be.<DeviceMacro> getData());
                  macroWindow.hide();
               }
            });
         }
      });
      macroToolBar.add(newMacroBtn);
      
      List<Button> editDelBtns = new ArrayList<Button>();
      Button editMacroBtn = new Button("Edit");
      editMacroBtn.setEnabled(false);
      editMacroBtn.setToolTip("Edit Macro");
      editMacroBtn.setIcon(icons.macroEditIcon());
      editMacroBtn.addSelectionListener(new SelectionListener<ButtonEvent>() {
         @Override
         public void componentSelected(ButtonEvent ce) {
            onEditDeviceMacroBtnClicked();

         }
      });
      macroToolBar.add(editMacroBtn);
      Button deleteMacroBtn = new Button("Delete");
      deleteMacroBtn.setEnabled(false);
      deleteMacroBtn.setToolTip("Delete Macro");
      deleteMacroBtn.setIcon(icons.macroDeleteIcon());
      
      deleteMacroBtn.addSelectionListener(new ConfirmDeleteListener<ButtonEvent>() {
         @Override
         public void onDelete(ButtonEvent ce) {
            onDeleteDeviceMacroBtnClicked();
         }
      });
      macroToolBar.add(deleteMacroBtn);
      editDelBtns.add(editMacroBtn);
      editDelBtns.add(deleteMacroBtn);
      selectionService.addListener(new EditDelBtnSelectionListener(editDelBtns));
      setTopComponent(macroToolBar);
   }

   /**
    * Creates the macro tree.
    */
   private void createMacroTree() {
      macroListContainer = new LayoutContainer() {
         @Override
         protected void onRender(Element parent, int index) {
            super.onRender(parent, index);
            if (macroTree == null) {
               macroTree = TreePanelBuilder.buildMacroTree();
               selectionService.addListener(new SourceSelectionChangeListenerExt(macroTree.getSelectionModel()));
               selectionService.register(macroTree.getSelectionModel());
               addTreeStoreEventListener();
               macroListContainer.add(macroTree);
            }
            add(macroTree);
            macroTree.addListener(DoubleClickEvent.DOUBLECLICK, new Listener<DoubleClickEvent>() {
               public void handleEvent(DoubleClickEvent be) {
                  onEditDeviceMacroBtnClicked();
               }
               
            });
         }
      };
      // overflow-auto style is for IE hack.
      macroListContainer.addStyleName("overflow-auto");
      macroListContainer.setStyleAttribute("backgroundColor", "white");
      macroListContainer.setBorders(false);
      macroListContainer.setLayoutOnChange(true);
      macroListContainer.setHeight("100%");
      add(macroListContainer);
   }

   /**
    * Adds the tree store event listener.
    */
   private void addTreeStoreEventListener() {
      macroTree.getStore().addListener(Store.Add, new Listener<TreeStoreEvent<BeanModel>>() {
         public void handleEvent(TreeStoreEvent<BeanModel> be) {
            addChangeListenerToDragSource(be.getChildren());
         }
      });
      macroTree.getStore().addListener(Store.DataChanged, new Listener<TreeStoreEvent<BeanModel>>() {
         public void handleEvent(TreeStoreEvent<BeanModel> be) {
            addChangeListenerToDragSource(be.getChildren());
         }
      });
      macroTree.getStore().addListener(Store.Clear, new Listener<TreeStoreEvent<BeanModel>>() {
         public void handleEvent(TreeStoreEvent<BeanModel> be) {
            removeChangeListenerToDragSource(be.getChildren());
         }
      });
      macroTree.getStore().addListener(Store.Remove, new Listener<TreeStoreEvent<BeanModel>>() {
         public void handleEvent(TreeStoreEvent<BeanModel> be) {
            removeChangeListenerToDragSource(be.getChildren());
         }
      });
   }

   /**
    * Adds the change listener to drag source.
    * 
    * @param models
    *           the models
    */
   private void addChangeListenerToDragSource(List<BeanModel> models) {
      if (models == null) {
         return;
      }
      for (BeanModel beanModel : models) {
         if (beanModel.getBean() instanceof DeviceMacroRef) {
            BeanModelDataBase.deviceMacroTable.addChangeListener(BeanModelDataBase
                  .getOriginalDeviceMacroItemBeanModelId(beanModel), getDragSourceBeanModelChangeListener(beanModel));
         } else if (beanModel.getBean() instanceof DeviceCommandRef) {
            BeanModelDataBase.deviceCommandTable.addChangeListener(BeanModelDataBase
                  .getOriginalDeviceMacroItemBeanModelId(beanModel), getDragSourceBeanModelChangeListener(beanModel));
            BeanModelDataBase.deviceTable.addChangeListener(BeanModelDataBase.getSourceBeanModelId(beanModel),
                  getDragSourceBeanModelChangeListener(beanModel));
         }
      }
   }

   /**
    * Removes the change listener to drag source.
    * 
    * @param models
    *           the models
    */
   private void removeChangeListenerToDragSource(List<BeanModel> models) {
      if (models == null) {
         return;
      }
      for (BeanModel beanModel : models) {
         if (beanModel.getBean() instanceof DeviceMacroRef) {
            BeanModelDataBase.deviceMacroTable.removeChangeListener(BeanModelDataBase
                  .getOriginalDeviceMacroItemBeanModelId(beanModel), getDragSourceBeanModelChangeListener(beanModel));
         }
         if (beanModel.getBean() instanceof DeviceCommandRef) {
            BeanModelDataBase.deviceCommandTable.removeChangeListener(BeanModelDataBase
                  .getOriginalDeviceMacroItemBeanModelId(beanModel), getDragSourceBeanModelChangeListener(beanModel));
         }
         changeListenerMap.remove(beanModel);
      }
   }

   /**
    * After create device macro.
    * 
    * @param deviceMacro
    *           the device macro
    */
   private void afterCreateDeviceMacro(DeviceMacro deviceMacro) {
      BeanModel deviceBeanModel = deviceMacro.getBeanModel();
      macroTree.getStore().add(deviceBeanModel, false);
      macroTree.setExpanded(deviceBeanModel, true);
   }

   /**
    * On edit device macro btn clicked.
    */
   private void onEditDeviceMacroBtnClicked() {
      if (macroTree.getSelectionModel().getSelectedItem() != null && macroTree.getSelectionModel().getSelectedItem().getBean() instanceof DeviceMacro) {
         final BeanModel oldModel = macroTree.getSelectionModel().getSelectedItem();
         final MacroWindow macroWindow = new MacroWindow(macroTree.getSelectionModel().getSelectedItem());
         macroWindow.addListener(SubmitEvent.SUBMIT, new SubmitListener() {
            @Override
            public void afterSubmit(SubmitEvent be) {
               afterUpdateDeviceMacroSubmit(oldModel, be.<DeviceMacro> getData());
               macroWindow.hide();
            }
         });
      }
   }

   /**
    * On delete device macro btn clicked.
    */
   private void onDeleteDeviceMacroBtnClicked() {
      if (macroTree.getSelectionModel().getSelectedItems().size() > 0) {
         for (final BeanModel data : macroTree.getSelectionModel().getSelectedItems()) {
            if (data.getBean() instanceof DeviceMacro) {
               DeviceMacroBeanModelProxy.deleteDeviceMacro(data, new AsyncSuccessCallback<Void>() {
                  @Override
                  public void onSuccess(Void result) {
                     macroTree.getStore().remove(data);
                     Info.display("Info", "Delete success.");
                  }
               });
            }

         }
      }
   }

   /**
    * After update device macro submit.
    * 
    * @param dataModel
    *           the data model
    * @param deviceMacro
    *           the device macro
    */
   private void afterUpdateDeviceMacroSubmit(final BeanModel dataModel, DeviceMacro deviceMacro) {
      DeviceMacro old = dataModel.getBean();
      old.setName(deviceMacro.getName());
      if (old.getDeviceMacroItems() != null) {
         for (DeviceMacroItem item: old.getDeviceMacroItems()) {
            BeanModelDataBase.deviceMacroItemTable.delete(item.getBeanModel());
         }
      }
      if (deviceMacro != null) {
         BeanModelDataBase.deviceMacroItemTable.insertAll(DeviceMacroItem.createModels(deviceMacro.getDeviceMacroItems()));
      }
      old.setDeviceMacroItems(deviceMacro.getDeviceMacroItems());
      macroTree.getStore().removeAll(dataModel);
      macroTree.getStore().remove(dataModel);
      macroTree.getStore().getLoader().load();
   }

   /**
    * Gets the drag source bean model change listener.
    * 
    * @param target
    *           the target
    * 
    * @return the drag source bean model change listener
    */
   private ChangeListener getDragSourceBeanModelChangeListener(final BeanModel target) {
      if (changeListenerMap == null) {
         changeListenerMap = new HashMap<BeanModel, ChangeListener>();
      }
      ChangeListener changeListener = changeListenerMap.get(target);
      if (changeListener == null) {
         changeListener = new ChangeListener() {
            public void modelChanged(ChangeEvent changeEvent) {
               if (changeEvent.getType() == ChangeEventSupport.Remove) {
                  macroTree.getStore().remove(target);
               }
               if (changeEvent.getType() == ChangeEventSupport.Update) {
                  BeanModel source = (BeanModel) changeEvent.getItem();
                  if (source.getBean() instanceof DeviceMacro) {
                     DeviceMacro deviceMacro = (DeviceMacro) source.getBean();
                     DeviceMacroRef deviceMacroRef = (DeviceMacroRef) target.getBean();
                     deviceMacroRef.setTargetDeviceMacro(deviceMacro);
                  } else if (source.getBean() instanceof DeviceCommand) {
                     DeviceCommand deviceCommand = (DeviceCommand) source.getBean();
                     DeviceCommandRef deviceCommandRef = (DeviceCommandRef) target.getBean();
                     deviceCommandRef.setDeviceCommand(deviceCommand);
                  } else if (source.getBean() instanceof Device) {
                     Device device = (Device) source.getBean();
                     DeviceCommandRef targetDeviceCommandRef = (DeviceCommandRef) target.getBean();
                     targetDeviceCommandRef.setDeviceName(device.getName());
                  }
                  macroTree.getStore().update(target);
               }
            }
         };
         changeListenerMap.put(target, changeListener);
      }
      return changeListener;
   }

}
