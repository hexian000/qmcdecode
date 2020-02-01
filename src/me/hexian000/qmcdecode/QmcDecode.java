package me.hexian000.qmcdecode;

import javax.swing.*;
import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class QmcDecode extends JFrame {
    private JProgressBar decodeProgress;
    private JPanel contentPane;
    private JList<String> taskList;
    private JLabel dropLabel;

    private Timer refreshTimer;
    private DecodeWorker currentWorker;
    private Queue<DecodeWorker> queue = new LinkedList<>();

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

    private void addFiles(List<String> files) {
        for (String file : files) {
            try {
                var f = new File(file);
                if (!f.exists() || !f.isFile()) {
                    System.err.println(String.format("\"%s\" is not a file", file));
                    continue;
                }
                String inputName;
                String inputExt;
                {
                    if (!file.contains(".")) {
                        System.err.println(String.format("\"%s\" - no extension name found", file));
                        continue;
                    }
                    var index = file.lastIndexOf(".");
                    inputName = file.substring(0, index);
                    inputExt = file.substring(index).toLowerCase();
                }
                switch (inputExt) {
                    case ".qmc0":
                    case ".qmc3":
                        addTask(file, inputName + ".mp3");
                        break;
                    case ".qmcogg":
                        addTask(file, inputName + ".ogg");
                        break;
                    case ".qmcflac":
                        addTask(file, inputName + ".flac");
                        break;
                }
            } catch (Exception ex) {
                System.err.println(String.format("\"%s\" error: %s", file, ex.getMessage()));
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
            queue.remove(currentWorker);
            updateTaskList();
            nextWorker();
        }
    }

    public QmcDecode() {
        setContentPane(contentPane);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setTitle("qmc decode");

        dropLabel.setDropTarget(new FileDropTarget() {
            @Override
            public void onFileDrop(List<String> files) {
                addFiles(files);
            }
        });
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
