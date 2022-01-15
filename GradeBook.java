package com.company;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.FileWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;


public class GradeBook extends JFrame {
    private JPanel mainPanel;
    private JButton addButton;
    private JTable table;
    private JTextField ratingTextField;
    private JTextField nameTextField;
    private JLabel avgLabel;
    private JButton deleteButton;
    private JTextField genreTextField;
    private JButton rentButton;
    private JButton collectButton;
    private JTextField errTextField;
    private JButton saveButton;
    private JScrollPane scrollPane;
    private final DefaultTableModel model = new DefaultTableModel(new String[]{"Name", "Rating", "Genre", "Availability"}, 0);

    public GradeBook(){
        setupFrame();
        loadSource();

        nameTextField.addActionListener(e -> ratingTextField.grabFocus());
        ratingTextField.addActionListener(e -> genreTextField.grabFocus());
        genreTextField.addActionListener(e -> addMovieToTable());

        addButton.addActionListener(e -> addMovieToTable());
        deleteButton.addActionListener(e -> deleteRecordFromTable());
        rentButton.addActionListener(e -> rentMovie());
        collectButton.addActionListener(e -> collectMovie());
        saveButton.addActionListener(e -> saveData());
    }

    private void setupFrame(){
        setContentPane(mainPanel);
        //setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveData();

                JOptionPane pane = new JOptionPane("Saving...", JOptionPane.INFORMATION_MESSAGE);
                JDialog dialog = pane.createDialog("Autosave");
                dialog.addWindowListener(null);
                dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

                Timer timer = new Timer(1500, new ActionListener() { // 10 sec
                    public void actionPerformed(ActionEvent e) {
                        dialog.setVisible(false);
                        dialog.dispose();
                    }
                });

                timer.start();

                dialog.setVisible(true);
                System.exit(0);
            }
        });

        setMinimumSize(new Dimension(700, 700));
        setVisible(true);
        table.setModel(model);
        model.setColumnCount(4);
        setTitle("Video Rental Service");
        setIconImage(new ImageIcon("blockbuster-logo.png").getImage());
        scrollPane.setViewportView(table);
    }

    private void rentMovie(){
        try {
            String val = String.valueOf(table.getModel().getValueAt(table.convertRowIndexToModel(table.getSelectedRow()), 3));
            if (val.equals("available")) {
                table.getModel().setValueAt("unavailable", table.convertRowIndexToModel(table.getSelectedRow()), 3);
                errTextField.setText("Renting successful.");
                sortTable();
            }
            else {
                throw new Exception(val);
            }
        }
        catch(Exception e){
            errTextField.setText("Cannot rent already rented movie!");
            JOptionPane.showMessageDialog(mainPanel,
                    "Cannot rent already rented movie!",
                    "Warning",
                    JOptionPane.WARNING_MESSAGE);
        }

    }

    private void collectMovie(){
        try {
            String val = String.valueOf(table.getModel().getValueAt(table.convertRowIndexToModel(table.getSelectedRow()), 3));
            if (val.equals("unavailable")) {
                table.getModel().setValueAt("available", table.convertRowIndexToModel(table.getSelectedRow()), 3);
                errTextField.setText("Collection successful");
                sortTable();
            }
            else {
                throw new Exception(val);
            }
        }
        catch(Exception e){
            errTextField.setText("Cannot collect not rented movie!");
            JOptionPane.showMessageDialog(mainPanel,
                    "Cannot collect not rented movie!",
                    "Warning",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    private void sortTable(){
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);

        List<RowSorter.SortKey> sortKeys = new ArrayList<>();

        sortKeys.add(new RowSorter.SortKey(3, SortOrder.ASCENDING));
        sortKeys.add(new RowSorter.SortKey(0, SortOrder.ASCENDING));
        sortKeys.add(new RowSorter.SortKey(2, SortOrder.ASCENDING));
        sortKeys.add(new RowSorter.SortKey(1, SortOrder.DESCENDING));

        sorter.setSortKeys(sortKeys);
        sorter.sort();
        table.setRowSorter(sorter);
    }

    private void deleteRecordFromTable(){
        model.removeRow(table.convertRowIndexToModel(table.getSelectedRow()));
        updateAvg();
        nameTextField.grabFocus();
    }

    private void addMovieToTable(){
        String name = nameTextField.getText();
        String rating = ratingTextField.getText();
        String genre = genreTextField.getText();
        if (rating.contains("/")) {
            String numerator = rating.substring(0, rating.indexOf("/"));
            String denominator = rating.substring(rating.indexOf("/") + 1);
            Double ratingFl = Double.parseDouble(numerator) / Double.parseDouble(denominator);
            model.addRow(new Object[]{name, ratingFl, genre, "available"});
            sortTable();
            errTextField.setText("Adding successful.");
        }
        else
        {
            try {
                Double ratingFl = Double.parseDouble(rating);
                model.addRow(new Object[]{name, ratingFl, genre, "available"});
                errTextField.setText("Adding successful.");
            }
            catch(Exception e){
                errTextField.setText("Error while adding a new record. Format not supported.");
                JOptionPane.showMessageDialog(mainPanel,
                        "Error while adding a new record. Format not supported." + System.lineSeparator() + "Supported formats are: "
                                + System.lineSeparator() + "X/Y" + System.lineSeparator() + "X.X",
                        "Warning - unsupported format",
                        JOptionPane.WARNING_MESSAGE);
            }
        }

        updateAvg();

        nameTextField.setText("");
        ratingTextField.setText("");
        genreTextField.setText("");
        nameTextField.grabFocus();
    }

    private void updateAvg(){
        Double sum = 0.0;
        int rows = model.getRowCount();

        try {
            for (int i = 0; i < rows; i++) {
                sum += (Double) model.getValueAt(i, 1);
            }

            Double avg = sum / rows;
            avgLabel.setText(String.format("Average rating: %.2f", avg));
            errTextField.setText("Updating successful");
        }
        catch(Exception e){
            errTextField.setText("Error while updating average rating.");
            JOptionPane.showMessageDialog(mainPanel,
                    "Error while updating average rating.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadSource(){
        Path path = Paths.get("database.txt");
        Charset charset = StandardCharsets.UTF_8;
        List<String> lines = null;
        String[] splitted;

        try {
            lines = Files.readAllLines(path, charset);
            for (String line: lines){
                splitted = line.split(";");
                model.addRow(new Object[]{splitted[0], Double.parseDouble(splitted[1]), splitted[2], splitted[3]});
            }
            sortTable();
            errTextField.setText("No errors found.");
        }
        catch (Exception e) {
            errTextField.setText("Error while loading a database.");
            JOptionPane.showMessageDialog(mainPanel,
                    "Error while loading a database.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }

        updateAvg();

    }

    private void saveData(){

        int rows = model.getRowCount();
        int columns = model.getColumnCount();

        FileWriter database = null;
        try {
            database = new FileWriter("database.txt");
            for (int r = 0; r < rows; r++) {
                for(int c = 0; c < columns; c++) {
                    database.write(String.valueOf(model.getValueAt(r, c)));
                    database.write(";");
                }
                database.write(System.lineSeparator());
            }
            database.close();
            errTextField.setText("Saving successful");
        }
        catch (Exception e) {
            errTextField.setText("Error while saving database.");
            JOptionPane.showMessageDialog(mainPanel,
                    "Error while saving database. Process not successful.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
