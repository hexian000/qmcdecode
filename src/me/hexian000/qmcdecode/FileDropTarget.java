package me.hexian000.qmcdecode;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

public abstract class FileDropTarget extends DropTarget {
    private static List<File> getFileDropWindows(Transferable transferable) throws IOException, UnsupportedFlavorException {
        List<File> result = new ArrayList<>();
        List<?> data = (List<?>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
        for (Object o : data) {
            if (o instanceof File) {
                result.add((File) o);
            }
        }
        return Collections.unmodifiableList(result);
    }

    private static List<File> getFileDropUnix(Transferable transferable) throws IOException, UnsupportedFlavorException, ClassNotFoundException {
        List<File> result = new ArrayList<>();
        DataFlavor nixFileDataFlavor = new DataFlavor("text/uri-list;class=java.lang.String");
        String data = (String) transferable.getTransferData(nixFileDataFlavor);
        for (StringTokenizer st = new StringTokenizer(data, "\r\n"); st.hasMoreTokens(); ) {
            String token = st.nextToken().trim();
            if (token.startsWith("#") || token.isEmpty()) {
                // comment line, by RFC 2483
                continue;
            }
            try {
                var file = new File(new URI(token));
                result.add(file);
            } catch (Exception ignore) {
            }
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public synchronized void drop(DropTargetDropEvent dtde) {
        dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
        Transferable transferable = dtde.getTransferable();
        try {
            onFileDrop(getFileDropWindows(transferable));
            return;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        try {
            onFileDrop(getFileDropUnix(transferable));
            return;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        System.err.println("unsupported transferable");
    }

    public abstract void onFileDrop(List<File> files);
}
