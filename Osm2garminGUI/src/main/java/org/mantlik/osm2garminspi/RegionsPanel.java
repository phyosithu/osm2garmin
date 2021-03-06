/*
 * #%L
 * Osm2garminGUI
 * %%
 * Copyright (C) 2011 Frantisek Mantlik <frantisek at mantlik.cz>
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */
package org.mantlik.osm2garminspi;

import java.awt.Color;
import java.awt.Component;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import org.mantlik.osm2garmin.Osm2garmin;
import org.mantlik.osm2garmin.Region;
import org.mantlik.osm2garmin.Utilities;
import org.openide.awt.HtmlBrowser;
import org.openide.util.Exceptions;
import org.openide.util.NbPreferences;

final class RegionsPanel extends javax.swing.JPanel implements ListSelectionListener {

    private final RegionsOptionsPanelController controller;
    private int selectedRow = -1;

    RegionsPanel(RegionsOptionsPanelController controller) {
        this.controller = controller;
        initComponents();
        regionsTable.getSelectionModel().addListSelectionListener(this);
        regionsTable.setDefaultRenderer(Float.class, new PolygonRenderer());
        moveDownRegionButton.setEnabled(false);
        moveUpRegionButton.setEnabled(false);
        displayRegionButton.setEnabled(false);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel3 = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        regionsTable = new javax.swing.JTable(){
            public boolean isCellEditable(int rowIndex, int colIndex) {
                if (colIndex < 2) {
                    return true;
                }
                return polyNotExists(rowIndex);
            }
        };
        addRegionButton = new javax.swing.JButton();
        moveUpRegionButton = new javax.swing.JButton();
        moveDownRegionButton = new javax.swing.JButton();
        deleteRegionButton = new javax.swing.JButton();
        displayRegionButton = new javax.swing.JButton();

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder(null, org.openide.util.NbBundle.getMessage(RegionsPanel.class, "RegionsPanel.jPanel3.border.title"), javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, null, new java.awt.Color(51, 153, 255))); // NOI18N

        regionsTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Enabled", "Name", "Lat1", "Lon1", "Lat2", "Lon2"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Boolean.class, java.lang.String.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        regionsTable.setToolTipText(org.openide.util.NbBundle.getMessage(RegionsPanel.class, "RegionsPanel.regionsTable.toolTipText")); // NOI18N
        regionsTable.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_ALL_COLUMNS);
        regionsTable.getTableHeader().setReorderingAllowed(false);
        regionsTable.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                regionsTablePropertyChange(evt);
            }
        });
        jScrollPane3.setViewportView(regionsTable);
        regionsTable.getColumnModel().getColumn(0).setHeaderValue(org.openide.util.NbBundle.getMessage(RegionsPanel.class, "RegionsPanel.regionsTable.columnModel.title5")); // NOI18N
        regionsTable.getColumnModel().getColumn(1).setHeaderValue(org.openide.util.NbBundle.getMessage(RegionsPanel.class, "RegionsPanel.regionsTable.columnModel.title0_1")); // NOI18N
        regionsTable.getColumnModel().getColumn(2).setHeaderValue(org.openide.util.NbBundle.getMessage(RegionsPanel.class, "RegionsPanel.regionsTable.columnModel.title1_1")); // NOI18N
        regionsTable.getColumnModel().getColumn(3).setHeaderValue(org.openide.util.NbBundle.getMessage(RegionsPanel.class, "RegionsPanel.regionsTable.columnModel.title2_1")); // NOI18N
        regionsTable.getColumnModel().getColumn(4).setHeaderValue(org.openide.util.NbBundle.getMessage(RegionsPanel.class, "RegionsPanel.regionsTable.columnModel.title3_1")); // NOI18N
        regionsTable.getColumnModel().getColumn(5).setHeaderValue(org.openide.util.NbBundle.getMessage(RegionsPanel.class, "RegionsPanel.regionsTable.columnModel.title4_1")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(addRegionButton, org.openide.util.NbBundle.getMessage(RegionsPanel.class, "RegionsPanel.addRegionButton.text")); // NOI18N
        addRegionButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addRegionButtonActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(moveUpRegionButton, org.openide.util.NbBundle.getMessage(RegionsPanel.class, "RegionsPanel.moveUpRegionButton.text")); // NOI18N
        moveUpRegionButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                moveUpRegionButtonActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(moveDownRegionButton, org.openide.util.NbBundle.getMessage(RegionsPanel.class, "RegionsPanel.moveDownRegionButton.text")); // NOI18N
        moveDownRegionButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                moveDownRegionButtonActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(deleteRegionButton, org.openide.util.NbBundle.getMessage(RegionsPanel.class, "RegionsPanel.deleteRegionButton.text")); // NOI18N
        deleteRegionButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteRegionButtonActionPerformed(evt);
            }
        });

        displayRegionButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/mantlik/osm2garminspi/Globe.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(displayRegionButton, org.openide.util.NbBundle.getMessage(RegionsPanel.class, "RegionsPanel.displayRegionButton.text")); // NOI18N
        displayRegionButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                displayRegionButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 375, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(addRegionButton, javax.swing.GroupLayout.PREFERRED_SIZE, 105, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(moveUpRegionButton, javax.swing.GroupLayout.PREFERRED_SIZE, 105, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(moveDownRegionButton, javax.swing.GroupLayout.PREFERRED_SIZE, 105, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(deleteRegionButton, javax.swing.GroupLayout.PREFERRED_SIZE, 105, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(displayRegionButton, javax.swing.GroupLayout.PREFERRED_SIZE, 105, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(addRegionButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(moveUpRegionButton)
                        .addGap(5, 5, 5)
                        .addComponent(moveDownRegionButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(deleteRegionButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 45, Short.MAX_VALUE)
                        .addComponent(displayRegionButton))
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void addRegionButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addRegionButtonActionPerformed
        DefaultTableModel model = (DefaultTableModel) regionsTable.getModel();
        model.addRow(new Object[]{true, "new_region", 0.0f, 0.0f, 0.0f, 0.0f});
    }//GEN-LAST:event_addRegionButtonActionPerformed

    private void moveUpRegionButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_moveUpRegionButtonActionPerformed
        DefaultTableModel model = (DefaultTableModel) regionsTable.getModel();
        if (selectedRow > 0) {
            model.moveRow(selectedRow, selectedRow, selectedRow - 1);
            regionsTable.setRowSelectionInterval(selectedRow - 1, selectedRow - 1);
        }
    }//GEN-LAST:event_moveUpRegionButtonActionPerformed

    private void moveDownRegionButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_moveDownRegionButtonActionPerformed
        DefaultTableModel model = (DefaultTableModel) regionsTable.getModel();
        if (selectedRow < regionsTable.getRowCount() - 1) {
            model.moveRow(selectedRow, selectedRow, selectedRow + 1);
            regionsTable.setRowSelectionInterval(selectedRow + 1, selectedRow + 1);
        }
    }//GEN-LAST:event_moveDownRegionButtonActionPerformed

    private void deleteRegionButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteRegionButtonActionPerformed
        DefaultTableModel model = (DefaultTableModel) regionsTable.getModel();
        model.removeRow(selectedRow);
    }//GEN-LAST:event_deleteRegionButtonActionPerformed

    private void displayRegionButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_displayRegionButtonActionPerformed
        DefaultTableModel model = (DefaultTableModel) regionsTable.getModel();
        if ((selectedRow < 0) || (selectedRow >= regionsTable.getRowCount())) {
            return;
        }
        boolean enabled = (Boolean) model.getValueAt(selectedRow, 0);
        String name = (String) model.getValueAt(selectedRow, 1);
        float lat1 = (Float) model.getValueAt(selectedRow, 2);
        float lon1 = (Float) model.getValueAt(selectedRow, 3);
        float lat2 = (Float) model.getValueAt(selectedRow, 4);
        float lon2 = (Float) model.getValueAt(selectedRow, 5);
        float left = Math.max(lon1 - 5, -180);
        float right = Math.min(lon2 + 5, 180);
        float bottom = Math.max(lat1 - 5, -65);
        float top = Math.min(lat2 + 5, 85);
        String addr = "http://dev.openstreetmap.org/~pafciu17/?module=map&bbox="
                + left + "," + top + "," + right + "," + bottom
                + "&height=600&width=800&filledPolygons=" + lon1 + "," + lat1 + "," + lon2 + ","
                + lat1 + "," + lon2 + "," + lat2 + "," + lon1 + "," + lat2 + "," + "color:0:255:255,transparency:100";
        String rname = NbPreferences.forModule(Osm2garmin.class).get("userdir",
                System.getProperty("netbeans.user") + "/") + name.trim() + ".html";
        File r = new File(rname);
        try {
            PrintStream printer = new PrintStream(r);
            printer.println("<html><head><title>" + name + "</title></head><body>");
            printer.println("<img src=\"" + addr + "\" />");
            printer.println("</body></html>");
            printer.close();
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        try {
            URL aurl = new URL("file:///" + rname); // NOI18N
            // org.openide.awt.StatusDisplayer.getDefault().setStatusText("Opening browser...");
            HtmlBrowser.URLDisplayer.getDefault().showURL(aurl);
        } catch (MalformedURLException ex) {
            //ignore
        }
    }//GEN-LAST:event_displayRegionButtonActionPerformed

    private void regionsTablePropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_regionsTablePropertyChange
        controller.changed();
    }//GEN-LAST:event_regionsTablePropertyChange

    void load() {
        // TODO read settings and initialize GUI
        // Example:        
        // someCheckBox.setSelected(Preferences.userNodeForPackage(RegionsPanel.class).getBoolean("someFlag", false));
        // or for org.openide.util with API spec. version >= 7.4:
        // someCheckBox.setSelected(NbPreferences.forModule(RegionsPanel.class).getBoolean("someFlag", false));
        // or:
        // someTextField.setText(SomeSystemOption.getDefault().getSomeStringProperty());
        File r = new File(NbPreferences.forModule(Osm2garmin.class).get("regions",
                System.getProperty("netbeans.user") + "/" + "regions.txt"));
        if (!r.exists()) {
            try {
                Utilities.copyFile(Osm2garmin.class.getResourceAsStream("regions.txt"),
                        r);
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
        Scanner s;
        DefaultTableModel model = (DefaultTableModel) regionsTable.getModel();
        while (model.getRowCount() > 0) {
            model.removeRow(model.getRowCount() - 1);
        }
        File regDir = r.getParentFile();
        try {
            s = new Scanner(new FileInputStream(r));
            while (s.hasNext()) {
                String[] l = s.nextLine().split(" +");
                if (l.length >= 5 && !(l[0].startsWith("#"))) {
                    boolean enabled = true;
                    if (l[0].startsWith("x")) {
                        enabled = false;
                        l[0] = l[0].replace("x", "");
                    }
                    String name = l[4];
                    File polyFile = null;
                    if (regDir != null) {
                        polyFile = new File(regDir, name + ".poly");
                    }
                    float lon1, lat1, lon2, lat2;
                    if ((polyFile != null) && polyFile.exists()) {
                        float[] f = Region.envelope(polyFile);
                        lon1 = f[0];
                        lat1 = f[1];
                        lon2 = f[2];
                        lat2 = f[3];
                    } else {
                        lon1 = Float.parseFloat(l[0]);
                        lat1 = Float.parseFloat(l[1]);
                        lon2 = Float.parseFloat(l[2]);
                        lat2 = Float.parseFloat(l[3]);
                    }
                    model.insertRow(model.getRowCount(), new Object[]{enabled, name, lat1, lon1, lat2, lon2});
                }
            }
        } catch (FileNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        }

    }

    void store() {
        // TODO store modified settings
        // Example:
        // Preferences.userNodeForPackage(RegionsPanel.class).putBoolean("someFlag", someCheckBox.isSelected());
        // or for org.openide.util with API spec. version >= 7.4:
        // NbPreferences.forModule(RegionsPanel.class).putBoolean("someFlag", someCheckBox.isSelected());
        // or:
        // SomeSystemOption.getDefault().setSomeStringProperty(someTextField.getText());
        File r = new File(NbPreferences.forModule(Osm2garmin.class).get("regions",
                System.getProperty("netbeans.user") + "/" + "regions.txt"));
        try {
            DefaultTableModel model = (DefaultTableModel) regionsTable.getModel();
            PrintStream printer = new PrintStream(r);
            for (int i = 0; i < regionsTable.getRowCount(); i++) {
                boolean enabled = (Boolean) model.getValueAt(i, 0);
                String name = (String) model.getValueAt(i, 1);
                float lat1 = (Float) model.getValueAt(i, 2);
                float lon1 = (Float) model.getValueAt(i, 3);
                float lat2 = (Float) model.getValueAt(i, 4);
                float lon2 = (Float) model.getValueAt(i, 5);
                name = name.trim().replace(" ", "_");
                if (!enabled) {
                    printer.print("x");
                }
                if (!name.equals("")) {
                    printer.println(lon1 + " " + lat1 + " " + lon2 + " " + lat2 + " " + name);
                }
            }
            printer.close();
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    boolean valid() {
        // TODO check whether form is consistent and complete
        return true;
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addRegionButton;
    private javax.swing.JButton deleteRegionButton;
    private javax.swing.JButton displayRegionButton;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JButton moveDownRegionButton;
    private javax.swing.JButton moveUpRegionButton;
    private javax.swing.JTable regionsTable;
    // End of variables declaration//GEN-END:variables

    @Override
    public void valueChanged(ListSelectionEvent e) {
        int newRow = regionsTable.getSelectedRow();
        if (newRow != selectedRow) {
            selectedRow = newRow;
            if (selectedRow == -1) {
                moveDownRegionButton.setEnabled(false);
                moveUpRegionButton.setEnabled(false);
                displayRegionButton.setEnabled(false);
                deleteRegionButton.setEnabled(false);
            } else {
                if (selectedRow < regionsTable.getRowCount() - 1) {
                    moveDownRegionButton.setEnabled(true);
                } else {
                    moveDownRegionButton.setEnabled(false);
                }
                if (selectedRow > 0) {
                    moveUpRegionButton.setEnabled(true);
                } else {
                    moveUpRegionButton.setEnabled(false);
                }
                displayRegionButton.setEnabled(true);
                deleteRegionButton.setEnabled(true);
            }
        }

    }

    boolean polyNotExists(int row) {
        String region = (String) regionsTable.getModel().getValueAt(row, 1);
        File r = new File(NbPreferences.forModule(Osm2garmin.class).get("regions",
                System.getProperty("netbeans.user") + "/" + "regions.txt")).getParentFile();
        if (r == null) {
            return true;
        }
        File polyFile = new File(r, region + ".poly");
        return !polyFile.exists();
    }

    class PolygonRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            Color foreground = component.getForeground();
            if (!polyNotExists(row)) {
                component.setForeground(Color.lightGray);
            } else {
                component.setForeground(Color.BLACK);
            }
            return component;
        }
    }
}
