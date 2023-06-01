import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class FileEncryptionApp extends JFrame {
    private JTextField sourceDirTextField;
    private JTextField targetDirTextField;
    private JCheckBox encryptionCheckBox;
    private JCheckBox hideFileCheckBox;
    private JCheckBox zipCheckBox;
    private JButton startButton;

    public FileEncryptionApp() {
        setTitle("Dosya Şifreleme ve Taşıma Uygulaması");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800,600);
        setResizable(false);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayout(5, 1));

        JLabel sourceDirLabel = new JLabel("Kaynak Dizin:");
        sourceDirTextField = new JTextField();
        JButton sourceDirButton = new JButton("Gözat");
        sourceDirButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // Kaynak dizinini seçmek için dosya gezgini açılır.
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                int returnValue = fileChooser.showOpenDialog(null);
                if (returnValue == JFileChooser.APPROVE_OPTION) {
                    File selectedDir = fileChooser.getSelectedFile();
                    sourceDirTextField.setText(selectedDir.getAbsolutePath());
                }
            }
        });

        JLabel targetDirLabel = new JLabel("Hedef Dizin:");
        targetDirTextField = new JTextField();
        JButton targetDirButton = new JButton("Gözat");
        targetDirButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // Hedef dizinini seçmek için dosya gezgini açılır.
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                int returnValue = fileChooser.showOpenDialog(null);
                if (returnValue == JFileChooser.APPROVE_OPTION) {
                    File selectedDir = fileChooser.getSelectedFile();
                    targetDirTextField.setText(selectedDir.getAbsolutePath());
                }
            }
        });

        JPanel checkBoxPanel = new JPanel();
        encryptionCheckBox = new JCheckBox("Şifrele");
        hideFileCheckBox = new JCheckBox("Gizli Dosya Yap");
        zipCheckBox = new JCheckBox("Sıkıştır (ZIP)");
        checkBoxPanel.add(encryptionCheckBox);
        checkBoxPanel.add(hideFileCheckBox);
        checkBoxPanel.add(zipCheckBox);

        startButton = new JButton("Başlat");
        startButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // Başlat düğmesine tıklandığında taşıma işlemi başlatılır.
                String sourceDirPath = sourceDirTextField.getText();
                String targetDirPath = targetDirTextField.getText();
                boolean encryptFiles = encryptionCheckBox.isSelected();
                boolean hideFiles = hideFileCheckBox.isSelected();
                boolean zipFiles = zipCheckBox.isSelected();

                moveFiles(sourceDirPath, targetDirPath, encryptFiles, hideFiles, zipFiles);
            }
        });

        mainPanel.add(sourceDirLabel);
        mainPanel.add(sourceDirTextField);
        mainPanel.add(sourceDirButton);
        mainPanel.add(targetDirLabel);
        mainPanel.add(targetDirTextField);
        mainPanel.add(targetDirButton);
        mainPanel.add(checkBoxPanel);
        mainPanel.add(startButton);

        add(mainPanel);
        setVisible(true);
    }

    private void moveFiles(String sourceDirPath, String targetDirPath, boolean encryptFiles, boolean hideFiles, boolean zipFiles) {
        // Kaynak dizin ve hedef dizin doğrulaması yapılır.
        File sourceDir = new File(sourceDirPath);
        File targetDir = new File(targetDirPath);

        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            JOptionPane.showMessageDialog(null, "Geçerli bir kaynak dizini seçin.", "Hata", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!targetDir.exists() || !targetDir.isDirectory()) {
            JOptionPane.showMessageDialog(null, "Geçerli bir hedef dizini seçin.", "Hata", JOptionPane.ERROR_MESSAGE);
            return;
        }

        File[] files = sourceDir.listFiles();
        if (files != null && files.length > 0) {
            for (File file : files) {
                // Dosya seçimi ve uygun dosya kontrolü yapılır.
                if (file.isFile() && isSupportedFile(file)) {
                    String targetFileName = file.getName();

                    // Dosya şifreleme seçeneği işaretlenmişse dosya adı şifrelenir.
                    if (encryptFiles) {
                        targetFileName = encryptFileName(targetFileName);
                    }

                    // Gizli dosya yap seçeneği işaretlenmişse dosya adının başına "." eklenir.
                    if (hideFiles) {
                        targetFileName = "." + targetFileName;
                    }

                    File targetFile = new File(targetDirPath + File.separator + targetFileName);
                    try {
                        // Dosya taşıma işlemi gerçekleştirilir.
                        FileInputStream in = new FileInputStream(file);
                        FileOutputStream out = new FileOutputStream(targetFile);
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = in.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                        }
                        in.close();
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    // Sıkıştır (ZIP) seçeneği işaretlenmişse dosya ZIP formatında sıkıştırılır.
                    if (zipFiles) {
                        zipFile(targetFile);
                    }
                }
            }
            JOptionPane.showMessageDialog(null, "Dosyalar başarıyla taşındı.", "Bilgi", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(null, "Kaynak dizininde geçerli dosya bulunamadı.", "Uyarı", JOptionPane.WARNING_MESSAGE);
        }
    }

    private boolean isSupportedFile(File file) {
        // Desteklenen dosya türlerini kontrol eder.
        String fileName = file.getName();
        return fileName.toLowerCase().endsWith(".pdf") ||
                fileName.toLowerCase().endsWith(".doc") ||
                fileName.toLowerCase().endsWith(".txt");
    }

    private String encryptFileName(String fileName) {
        // Dosya adını şifrelemek için SHA-256 algoritması kullanılır.
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(fileName.getBytes());
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return fileName;
    }

    private String bytesToHex(byte[] bytes) {
        // Byte dizisini hexadecimal formata dönüştürür.
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    private void zipFile(File file) {
        // Dosyayı ZIP formatında sıkıştırır.
        try {
            String zipFileName = file.getAbsolutePath() + ".zip";
            FileOutputStream fos = new FileOutputStream(zipFileName);
            ZipOutputStream zos = new ZipOutputStream(fos);
            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[1024];
            zos.putNextEntry(new ZipEntry(file.getName()));
            int length;
            while ((length = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, length);
            }
            zos.closeEntry();
            fis.close();
            zos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new FileEncryptionApp();
            }
   });

    }
}