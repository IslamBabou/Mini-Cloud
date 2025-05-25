import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class SimpleCloudClient extends JFrame {

    private static final String API_BASE_URL = "http://localhost:8080/api/files";
    private static final File CLOUD_DIRECTORY = new File("C:\\Users\\Public\\Desktop\\Cloud");
    
    private JPanel filePanel;
    private JButton refreshButton;
    private JLabel statusLabel;
    private final Color EVEN_ROW_COLOR = new Color(240, 240, 240);
    private final Color ODD_ROW_COLOR = new Color(255, 255, 255);
    private final Color HOVER_COLOR = new Color(230, 240, 250);
    
    private List<FileItem> fileItems = new ArrayList<>();
    
    private static class FileItem {
        Long id;
        String filename;
        
        FileItem(Long id, String filename) {
            this.id = id;
            this.filename = filename;
        }
    }

    public SimpleCloudClient() {
        setTitle("Cloud File Manager");
        setSize(600, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        initUI();
        loadFilesFromServer();
    }

    private void initUI() {
        setLayout(new BorderLayout());
        
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        headerPanel.setBackground(new Color(51, 102, 153));
        
        JLabel titleLabel = new JLabel("Cloud File Manager");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel, BorderLayout.WEST);
        
        refreshButton = new JButton("Refresh Files");
        refreshButton.setFocusPainted(false);
        refreshButton.addActionListener(e -> loadFilesFromServer());
        headerPanel.add(refreshButton, BorderLayout.EAST);
        
        add(headerPanel, BorderLayout.NORTH);
        
        filePanel = new JPanel();
        filePanel.setLayout(new BoxLayout(filePanel, BoxLayout.Y_AXIS));
        filePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JScrollPane scrollPane = new JScrollPane(filePanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);
        
        statusLabel = new JLabel("Ready");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        add(statusLabel, BorderLayout.SOUTH);
        
        setupMenuBar();
    }
    
    private void setupMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        
        JMenuItem uploadItem = new JMenuItem("Upload File");
        uploadItem.addActionListener(e -> showUploadDialog());
        fileMenu.add(uploadItem);
        
        JMenuItem refreshItem = new JMenuItem("Refresh");
        refreshItem.addActionListener(e -> loadFilesFromServer());
        fileMenu.add(refreshItem);
        
        fileMenu.addSeparator();
        
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(exitItem);
        
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);
    }

    private void loadFilesFromServer() {
        statusLabel.setText("Loading files...");
        refreshButton.setEnabled(false);
        
        SwingWorker<List<FileItem>, Void> worker = new SwingWorker<List<FileItem>, Void>() {
            @Override
            protected List<FileItem> doInBackground() throws Exception {
                List<FileItem> files = new ArrayList<>();
                try {
                    URL url = new URL(API_BASE_URL + "/list");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    
                    int responseCode = conn.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        BufferedReader reader = new BufferedReader(
                                new InputStreamReader(conn.getInputStream()));
                        StringBuilder json = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            json.append(line);
                        }
                        reader.close();
                        
                        String jsonStr = json.toString();
                        int startIdx = 0;
                        while ((startIdx = jsonStr.indexOf("{", startIdx)) != -1) {
                            int endIdx = jsonStr.indexOf("}", startIdx);
                            if (endIdx == -1) break;
                            
                            String fileObject = jsonStr.substring(startIdx, endIdx + 1);
                            Long id = null;
                            String filename = null;
                            
                            int idStart = fileObject.indexOf("\"id\":");
                            if (idStart != -1) {
                                idStart += "\"id\":".length();
                                int idEnd = fileObject.indexOf(",", idStart);
                                if (idEnd == -1) idEnd = fileObject.indexOf("}", idStart);
                                if (idEnd != -1) {
                                    try {
                                        id = Long.parseLong(fileObject.substring(idStart, idEnd).trim());
                                    } catch (NumberFormatException e) {}
                                }
                            }
                            
                            int filenameStart = fileObject.indexOf("\"filename\":\"");
                            if (filenameStart != -1) {
                                filenameStart += "\"filename\":\"".length();
                                int filenameEnd = fileObject.indexOf("\"", filenameStart);
                                if (filenameEnd != -1) {
                                    filename = fileObject.substring(filenameStart, filenameEnd);
                                }
                            }
                            
                            if (id != null && filename != null) {
                                files.add(new FileItem(id, filename));
                            }
                            
                            startIdx = endIdx + 1;
                        }
                    } else {
                        throw new IOException("Server returned code: " + responseCode);
                    }
                } catch (IOException e) {
                    SwingUtilities.invokeLater(() -> 
                        JOptionPane.showMessageDialog(SimpleCloudClient.this, 
                            "Error loading files: " + e.getMessage(), 
                            "Connection Error", 
                            JOptionPane.ERROR_MESSAGE)
                    );
                }
                return files;
            }

            @Override
            protected void done() {
                try {
                    fileItems = get();
                    updateFileList();
                } catch (Exception e) {
                    statusLabel.setText("Error: " + e.getMessage());
                } finally {
                    refreshButton.setEnabled(true);
                }
            }
        };
        worker.execute();
    }
    
    private void updateFileList() {
        filePanel.removeAll();
        
        if (fileItems.isEmpty()) {
            JLabel emptyLabel = new JLabel("No files found on server");
            emptyLabel.setFont(new Font("Arial", Font.ITALIC, 14));
            emptyLabel.setForeground(Color.GRAY);
            emptyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            filePanel.add(Box.createVerticalGlue());
            filePanel.add(emptyLabel);
            filePanel.add(Box.createVerticalGlue());
            statusLabel.setText("No files available");
        } else {
            JPanel headerRow = new JPanel(new BorderLayout());
            headerRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
            headerRow.setBackground(new Color(230, 230, 230));
            headerRow.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.GRAY));
            
            JLabel fileHeaderLabel = new JLabel("File Name");
            fileHeaderLabel.setFont(new Font("Arial", Font.BOLD, 14));
            fileHeaderLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
            headerRow.add(fileHeaderLabel, BorderLayout.CENTER);
            
            JLabel actionHeaderLabel = new JLabel("Action");
            actionHeaderLabel.setFont(new Font("Arial", Font.BOLD, 14));
            actionHeaderLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
            headerRow.add(actionHeaderLabel, BorderLayout.EAST);
            
            filePanel.add(headerRow);
            
            int index = 0;
            for (FileItem fileItem : fileItems) {
                addFileEntry(fileItem, index++ % 2 == 0);
            }
            statusLabel.setText(fileItems.size() + " files loaded successfully");
        }
        
        filePanel.revalidate();
        filePanel.repaint();
    }

    private void addFileEntry(FileItem fileItem, boolean isEven) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        panel.setBackground(isEven ? EVEN_ROW_COLOR : ODD_ROW_COLOR);
        panel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(220, 220, 220)));
        
        JLabel fileLabel = new JLabel(fileItem.filename);
        fileLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        fileLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        panel.add(fileLabel, BorderLayout.CENTER);
        
        JButton downloadButton = new JButton("Download");
        downloadButton.setFocusPainted(false);
        downloadButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        downloadButton.addActionListener(e -> downloadFile(fileItem));
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setOpaque(false);
        buttonPanel.add(downloadButton);
        panel.add(buttonPanel, BorderLayout.EAST);
        
        MouseAdapter hoverAdapter = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent evt) {
                panel.setBackground(HOVER_COLOR);
            }
            
            @Override
            public void mouseExited(MouseEvent evt) {
                panel.setBackground(isEven ? EVEN_ROW_COLOR : ODD_ROW_COLOR);
            }
        };
        
        panel.addMouseListener(hoverAdapter);
        filePanel.add(panel);
    }

    private void downloadFile(FileItem fileItem) {
        statusLabel.setText("Downloading " + fileItem.filename + "...");
        
        SwingWorker<Boolean, Integer> worker = new SwingWorker<Boolean, Integer>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                try {
                    URL url = new URL(API_BASE_URL + "/download/" + fileItem.id);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    
                    int responseCode = conn.getResponseCode();
                    if (responseCode != HttpURLConnection.HTTP_OK) {
                        throw new IOException("Server returned code: " + responseCode);
                    }
                    
                    // Ensure cloud directory exists
                    if (!CLOUD_DIRECTORY.exists()) {
                        CLOUD_DIRECTORY.mkdirs();
                    }
                    
                    // Set default save location
                    File targetFile = new File(CLOUD_DIRECTORY, fileItem.filename);
                    
                    try (InputStream inputStream = conn.getInputStream();
                         FileOutputStream outputStream = new FileOutputStream(targetFile)) {
                        
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        long totalBytesRead = 0;
                        int contentLength = conn.getContentLength();
                        
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                            totalBytesRead += bytesRead;
                            
                            if (contentLength > 0) {
                                int progress = (int) ((totalBytesRead * 100) / contentLength);
                                publish(progress);
                            }
                        }
                    }
                    return true;
                    
                } catch (IOException ex) {
                    throw ex;
                }
            }
            
            @Override
            protected void process(List<Integer> chunks) {
                int latestProgress = chunks.get(chunks.size() - 1);
                statusLabel.setText("Downloading " + fileItem.filename + "... " + latestProgress + "%");
            }
            
            @Override
            protected void done() {
                try {
                    boolean completed = get();
                    if (completed) {
                        statusLabel.setText("Downloaded " + fileItem.filename + " successfully");
                    } else {
                        statusLabel.setText("Download canceled");
                    }
                } catch (Exception e) {
                    statusLabel.setText("Download failed: " + e.getMessage());
                    JOptionPane.showMessageDialog(SimpleCloudClient.this, 
                        "Download failed: " + e.getMessage(), 
                        "Download Error", 
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }
    
    private void showUploadDialog() {
        try {
            JFileChooser fileChooser = new JFileChooser(CLOUD_DIRECTORY);
            fileChooser.setDialogTitle("Select File to Upload");
            
            if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                
                String username = JOptionPane.showInputDialog(this, 
                    "Enter your username:", 
                    "Username Required", 
                    JOptionPane.QUESTION_MESSAGE);
                    
                if (username != null && !username.trim().isEmpty()) {
                    uploadFile(selectedFile, username);
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "Error initializing file dialog: " + e.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void uploadFile(File file, String username) {
        statusLabel.setText("Uploading " + file.getName() + "...");
        
        SwingWorker<Boolean, Integer> worker = new SwingWorker<Boolean, Integer>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                try {
                    String boundary = "---------------------------" + System.currentTimeMillis();
                    URL url = new URL(API_BASE_URL + "/upload");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setDoOutput(true);
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                    
                    long fileSize = file.length();
                    
                    try (OutputStream outputStream = conn.getOutputStream();
                         PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, "UTF-8"), true)) {
                             
                        writer.append("--" + boundary).append("\r\n");
                        writer.append("Content-Disposition: form-data; name=\"username\"").append("\r\n");
                        writer.append("\r\n");
                        writer.append(username).append("\r\n");
                        writer.flush();
                        
                        writer.append("--" + boundary).append("\r\n");
                        writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"").append("\r\n");
                        writer.append("Content-Type: application/octet-stream").append("\r\n");
                        writer.append("\r\n");
                        writer.flush();
                        
                        try (FileInputStream inputStream = new FileInputStream(file)) {
                            byte[] buffer = new byte[4096];
                            int bytesRead;
                            long totalBytesRead = 0;
                            
                            while ((bytesRead = inputStream.read(buffer)) != -1) {
                                outputStream.write(buffer, 0, bytesRead);
                                totalBytesRead += bytesRead;
                                
                                int progress = (int) ((totalBytesRead * 100) / fileSize);
                                publish(progress);
                            }
                        }
                        
                        writer.append("\r\n");
                        writer.append("--" + boundary + "--").append("\r\n");
                        writer.flush();
                    }
                    
                    int responseCode = conn.getResponseCode();
                    if (responseCode != HttpURLConnection.HTTP_OK) {
                        try (BufferedReader reader = new BufferedReader(
                                new InputStreamReader(conn.getErrorStream()))) {
                            String line;
                            StringBuilder errorResponse = new StringBuilder();
                            while ((line = reader.readLine()) != null) {
                                errorResponse.append(line);
                            }
                            throw new IOException("Server returned code: " + responseCode + 
                                                  " - " + errorResponse.toString());
                        }
                    }
                    
                    return true;
                    
                } catch (IOException ex) {
                    throw ex;
                }
            }
            
            @Override
            protected void process(List<Integer> chunks) {
                int latestProgress = chunks.get(chunks.size() - 1);
                statusLabel.setText("Uploading " + file.getName() + "... " + latestProgress + "%");
            }
            
            @Override
            protected void done() {
                try {
                    boolean completed = get();
                    if (completed) {
                        statusLabel.setText("Uploaded " + file.getName() + " successfully");
                        loadFilesFromServer();
                    } else {
                        statusLabel.setText("Upload failed");
                    }
                } catch (Exception e) {
                    statusLabel.setText("Upload failed: " + e.getMessage());
                    JOptionPane.showMessageDialog(SimpleCloudClient.this, 
                        "Upload failed: " + e.getMessage(), 
                        "Upload Error", 
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            SimpleCloudClient client = new SimpleCloudClient();
            client.setVisible(true);
        });
    }
}