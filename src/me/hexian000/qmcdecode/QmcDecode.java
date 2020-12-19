package me.hexian000.qmcdecode;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ExecutionException;

public class QmcDecode extends JFrame {
    private JProgressBar decodeProgress;
    private JPanel contentPane;
    private JList<String> taskList;
    private JLabel dropLabel;
    final JFileChooser chooser;

    private Timer refreshTimer;
    private DecodeWorker currentWorker;
    private final Queue<DecodeWorker> queue = new LinkedList<>();

    private void nextWorker() {
        currentWorker = queue.peek();
        if (currentWorker == null) {
            // no work to do
            return;
        }
        currentWorker.execute();
        decodeProgress.setValue(currentWorker.getProgress());
        if (refreshTimer == null) {
            refreshTimer = new Timer(100, e -> onTimer());
            refreshTimer.setRepeats(true);
            refreshTimer.start();
        }
    }

    private void updateTaskList() {
        DefaultListModel<String> model = new DefaultListModel<>();
        for (var task : queue) {
            model.addElement(task.getInput());
        }
        taskList.setModel(model);
        if (model.size() > 0) {
            taskList.setSelectedIndex(0);
        }
    }

    private void addTask(String input, String output) {
        var worker = new DecodeWorker(input, output);
        queue.add(worker);
        updateTaskList();
        if (currentWorker == null) {
            nextWorker();
        }
    }

    private void addFiles(List<File> files) {
        for (File file : files) {
            String filePath = file.getAbsolutePath();
            try {
                if (!file.exists() || !file.isFile()) {
                    System.err.printf("\"%s\" is not a file%n", filePath);
                    continue;
                }
                String inputName;
                String inputExt;
                {
                    if (!file.getName().contains(".")) {
                        System.err.printf("\"%s\" - no extension name found%n", filePath);
                        continue;
                    }
                    var index = filePath.lastIndexOf(".");
                    inputName = filePath.substring(0, index);
                    inputExt = filePath.substring(index).toLowerCase();
                }
                switch (inputExt) {
                    case ".qmc0":
                    case ".qmc3":
                        addTask(filePath, inputName + ".mp3");
                        break;
                    case ".qmcogg":
                        addTask(filePath, inputName + ".ogg");
                        break;
                    case ".qmcflac":
                        addTask(filePath, inputName + ".flac");
                        break;
                }
            } catch (Exception ex) {
                System.err.printf("\"%s\" error: %s%n", filePath, ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    private void onTimer() {
        if (currentWorker == null) {
            // no works running
            if (refreshTimer != null) {
                refreshTimer.stop();
                refreshTimer = null;
            }
            return;
        }
        decodeProgress.setValue(currentWorker.getProgress());
        if (currentWorker.isDone()) {
            try {
                currentWorker.get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, ex.getCause().getLocalizedMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
            queue.remove(currentWorker);
            updateTaskList();
            nextWorker();
        }
    }

    public void chooseFile() {
        var result = chooser.showOpenDialog(QmcDecode.this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }
        var files = chooser.getSelectedFiles();
        addFiles(Arrays.asList(files));
    }

    public QmcDecode() {
        setContentPane(contentPane);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setTitle("qmc decode");

        dropLabel.setDropTarget(new FileDropTarget() {
            @Override
            public void onFileDrop(List<File> files) {
                addFiles(files);
            }
        });
        dropLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                chooseFile();
            }
        });
        chooser = new JFileChooser();
        chooser.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                if (!f.isFile()) {
                    return false;
                }
                var path = f.getName();
                var index = path.lastIndexOf(".");
                if (index == -1) {
                    return false;
                }
                var ext = path.substring(index).toLowerCase();
                switch (ext) {
                    case ".qmc0":
                    case ".qmc3":
                    case ".qmcogg":
                    case ".qmcflac":
                        break;
                    default:
                        return false;
                }
                return true;
            }

            @Override
            public String getDescription() {
                return "QMC files";
            }
        });
        chooser.setMultiSelectionEnabled(true);
    }

    public static void main(String[] args) {
        try {
            // Set System L&F
            UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName());
        } catch (UnsupportedLookAndFeelException |
                ClassNotFoundException |
                InstantiationException |
                IllegalAccessException e) {
            e.printStackTrace();
        }

        final var frame = new QmcDecode();
        frame.setSize(640, 480);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
