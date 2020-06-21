/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.tantanreader;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDNamedDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.apache.pdfbox.printing.PDFPageable;
import org.apache.pdfbox.rendering.PDFRenderer;

/**
 *
 * @author cesar
 */
public class PDFReader extends java.awt.Panel {

    PDFRenderer pdfRenderer;
    PDDocument document;
    PDPage page;
    BufferedImage image;
    String path;
    JLabel[] pageRenderedImage;
    float renderSize = 1; //image size
    int renderRotationAngle = 0; //
    JButton buttons[];
    int currentPage;
    int leftPaneWidth;
    int miniatureWidth = -1, miniatureHeight = -1;
    int miniaturesVerticalScrollBarValue = 0;
    boolean miniaturesVerticalBarMouseReleased = false, pageVerticalBarMouseReleased = false;
    boolean hasLoadedDocument = false;
    boolean BookMode = false;
    boolean isCtrlPressed = false;
    ImageIcon whiteIcon;
    int[] bookmarksPages;
    int documentSize;
    boolean isLoadingDocument = false;
    public static final Logger logger = Logger.getLogger(PDFReader.class.getName());
    String readMode = ""; //pdf or zip
    ZipFile zipFile;
    ZipEntry[] zipEntries;

    /**
     * Creates new form PDFReader
     *
     * @throws java.io.IOException
     */
    public PDFReader() throws IOException {
        initComponents();

        FileHandler fh = new FileHandler(System.getProperty("user.home") + File.separator + "log.txt", false);
        fh.setFormatter(new SimpleFormatter());
        logger.addHandler(fh);
        logger.log(Level.INFO, "Starting program...");

        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("com/github/tantanreader/bundles/Bundle");
        jTabbedPane1.setTitleAt(0, bundle.getString("Main.tab1.text"));
        jTabbedPane1.setTitleAt(1, bundle.getString("Main.tab2.text"));

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) jTree1.getModel().getRoot();
        node.removeAllChildren();

        leftPaneWidth = jTabbedPane1.getWidth();
        TogglePanel();

        //set jtabbed pane tabs to fill pane width
            for (int i = 0; i < 2; i++) {
                String name = jTabbedPane1.getTitleAt(i);
                jTabbedPane1.setTitleAt(i, "<html><div style=\"width: " + 51 + "px\">" + name + "</div></html>");
            }

        scrollPanePage.requestFocus();

        scrollPanePage.getViewport().addChangeListener(new PDFReader.ListenAdditionsScrolled());
        scrollPaneMiniatures.getVerticalScrollBar().addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
            }

            @Override
            public void mousePressed(MouseEvent e) {
                miniaturesVerticalBarMouseReleased = true;
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                miniaturesVerticalBarMouseReleased = true;
                try {
                    if ((scrollPaneMiniatures.getVerticalScrollBar().getValue() != miniaturesVerticalScrollBarValue) && miniaturesVerticalBarMouseReleased) {
                        miniaturesVerticalScrollBarValue = scrollPaneMiniatures.getVerticalScrollBar().getValue();
                        miniaturesVerticalBarMouseReleased = false;
                        int maxValue = scrollPaneMiniatures.getVerticalScrollBar().getMaximum();
                        int pageSelected = (miniaturesVerticalScrollBarValue * documentSize) / maxValue;
                        setMiniatures(pageSelected + 2);
                    }
                } catch (Exception ex) {
                    System.out.println(ex);
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }
        });
        scrollPanePage.getVerticalScrollBar().addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
            }

            @Override
            public void mousePressed(MouseEvent e) {

            }

            @Override
            public void mouseReleased(MouseEvent e) {
                pageVerticalBarMouseReleased = true;
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }
        });
        scrollPanePage.getVerticalScrollBar().addAdjustmentListener((e) -> {
            int extent = scrollPanePage.getVerticalScrollBar().getModel().getExtent();
            if (((e.getValue() + extent) == scrollPanePage.getVerticalScrollBar().getMaximum()) && pageVerticalBarMouseReleased) {
                nextPage();
                miniaturesVerticalBarMouseReleased = false;
            }
        });
        jTree1.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent me) {
                TreePath tp = jTree1.getPathForLocation(me.getX(), me.getY());
                if (tp != null) {
                    Object[] path1 = tp.getPath();
                    try {
                        int indexOfChild = jTree1.getModel().getIndexOfChild(path1[0], path1[1]);
                        if (BookMode == false) {
                            loadPage(bookmarksPages[indexOfChild]);
                        } else {
                            if (bookmarksPages[indexOfChild] % 2 != 0) {
                                loadPage(bookmarksPages[indexOfChild], bookmarksPages[indexOfChild] + 1);
                            } else {
                                loadPage(bookmarksPages[indexOfChild] - 1, bookmarksPages[indexOfChild]);
                            }
                        }
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, "Load bookmark error: ", e);
                    }
                } else {

                }
            }
        });

        logger.log(Level.INFO, "Program started.");

    }

    public class ListenAdditionsScrolled implements ChangeListener {

        @Override
        public void stateChanged(ChangeEvent e) {
        }
    }

    public void loadFile(String Path) {
        logger.log(Level.INFO, "Loading file {0}", Path);
        isLoadingDocument = true;
        File file = new File(Path);
        BookMode = tbBookMode.isSelected();
        panel.removeAll();
        try {
            scrollPanePage.getViewport().setViewPosition(new Point(0, 0));
            if (readMode.equals("pdf")) {
                document = PDDocument.load(file);
                pdfRenderer = new PDFRenderer(document);
                documentSize = document.getNumberOfPages();
            } else if (readMode.equals("zip")) {
                try {
                    zipFile = new ZipFile(Path);
                    Enumeration<? extends ZipEntry> entries = zipFile.entries();
                    zipEntries = new ZipEntry[zipFile.size()];
                    int i = 0;
                    while (entries.hasMoreElements()) {
                        zipEntries[i] = entries.nextElement();
                        i++;
                    }
                    documentSize = zipFile.size();
                } catch (IOException ex) {
                    Logger.getLogger(PDFReader.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            lblLastPage.setText(String.valueOf(documentSize));

            if (BookMode == false) {
                setupPageViewGrid(0);
            } else {
                if (documentSize > 1) {
                    System.out.println("loadfile");
                    setupPageViewGrid(-1, 0);
                } else {
                    BookMode = false;
                    tbBookMode.setSelected(false);
                    loadFile(Path);
                }
            }
            setMiniaturesGrid();
            setMiniatures(currentPage);
            setupBookmarks();
            hasLoadedDocument = true;
            isLoadingDocument = false;
            logger.log(Level.INFO, "File loaded");
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Load PDF error: ", ex);
        }

    }

    private void setupBookmarks() {
        if (readMode.equals("pdf")) {
            PDDocumentOutline outline = document.getDocumentCatalog().getDocumentOutline();
            String indentation = "";
            DefaultMutableTreeNode root = (DefaultMutableTreeNode) jTree1.getModel().getRoot();
            root.removeAllChildren();
            DefaultMutableTreeNode node = null;
            bookmarksPages = new int[1];
            int count = 0;
            try {
                if (outline != null) {
                    PDOutlineItem current = outline.getFirstChild();
                    while (current != null) {
                        PDDestination destination = current.getDestination();
                        if (current.getAction() instanceof PDActionGoTo) {
                            PDActionGoTo gta = (PDActionGoTo) current.getAction();
                            if (gta.getDestination() instanceof PDPageDestination) {
                                count++;
                            } else if (gta.getDestination() instanceof PDNamedDestination) {
                                PDPageDestination pd = document.getDocumentCatalog().findNamedDestinationPage((PDNamedDestination) gta.getDestination());
                                if (pd != null) {
                                    count++;
                                }
                            }
                        }

                        current = current.getNextSibling();
                    }
                    current = outline.getFirstChild();
                    bookmarksPages = new int[count];
                    int i = 0;
                    while (current != null) {
                        PDDestination destination = current.getDestination();
                        if (current.getAction() instanceof PDActionGoTo) {
                            PDActionGoTo gta = (PDActionGoTo) current.getAction();
                            if (gta.getDestination() instanceof PDPageDestination) {
                                PDPageDestination pd = (PDPageDestination) gta.getDestination();
                                bookmarksPages[i] = pd.retrievePageNumber();
                                i++;
                                node = new DefaultMutableTreeNode(current.getTitle());
                                root.add(node);
                            } else if (gta.getDestination() instanceof PDNamedDestination) {
                                PDPageDestination pd = document.getDocumentCatalog().findNamedDestinationPage((PDNamedDestination) gta.getDestination());
                                if (pd != null) {
                                    bookmarksPages[i] = pd.retrievePageNumber();
                                    i++;
                                    node = new DefaultMutableTreeNode(current.getTitle());
                                    root.add(node);
                                }
                            }
                        }

                        current = current.getNextSibling();
                    }
                }
                DefaultTreeModel model = (DefaultTreeModel) jTree1.getModel();
                jTree1.setCellRenderer(
                        new DefaultTreeCellRenderer() {
                    @Override
                    public Color getTextNonSelectionColor() {
                        return new Color(240, 240, 240);
                    }
                });
                model.reload();

            } catch (IOException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        }
    }

    private void setupPageViewGrid(int renderImage) {
        try {
            panel.removeAll();
            GridBagLayout grid = new GridBagLayout();
            panel.setLayout(grid);
            if (readMode.equals("pdf")) {
                image = pdfRenderer.renderImage(renderImage, renderSize);
            } else if (readMode.equals("zip")) {
                image = ImageIO.read(zipFile.getInputStream(zipEntries[0]));
                Image im = image.getScaledInstance((int) ((int) image.getWidth() * renderSize), (int) ((int) image.getHeight() * renderSize), Image.SCALE_DEFAULT);
                image = new BufferedImage(im.getWidth(null), im.getHeight(null), BufferedImage.TYPE_INT_RGB);
                Graphics bg = image.getGraphics();
                bg.drawImage(im, 0, 0, null);
                bg.dispose();
            }
            if (tbNightMode.isSelected()) {
                image = invertColors(image);
            }
            if (renderRotationAngle != 0) {
                image = rotateImage(image, renderRotationAngle);
            }
            pageRenderedImage = new JLabel[1];
            pageRenderedImage[0] = new JLabel();
            pageRenderedImage[0].setText("");
            pageRenderedImage[0].setVisible(true);
            pageRenderedImage[0].setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
            ImageIcon icon = new ImageIcon(image);
            pageRenderedImage[0].setIcon(icon);
            panel.add(pageRenderedImage[0]);
            setCurrentPage(renderImage);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }

    private void setupPageViewGrid(int renderImage1, int renderImage2) {
        try {
            panel.removeAll();
            GridLayout grid = new GridLayout(1, 2, 0, 0);
            panel.setLayout(grid);
            pageRenderedImage = new JLabel[2];

            pageRenderedImage[0] = new JLabel();
            pageRenderedImage[0].setText("");
            pageRenderedImage[0].setVisible(true);

            pageRenderedImage[1] = new JLabel();
            pageRenderedImage[1].setText("");
            pageRenderedImage[1].setVisible(true);

            loadPage(renderImage1, renderImage2);

            panel.add(pageRenderedImage[0]);
            panel.add(pageRenderedImage[1]);

            pageRenderedImage[0].setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
            pageRenderedImage[1].setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
            if (renderImage1 < 0) {
                System.out.println("renderimage");
                setCurrentPage(0);
            } else {
                setCurrentPage(renderImage1);
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }

    private void loadPage(int page) {
        try {
            if (page >= 0 && page < documentSize) {
                if (readMode.equals("pdf")) {
                    image = pdfRenderer.renderImage(page, renderSize);
                } else if (readMode.equals("zip")) {
                    image = ImageIO.read(zipFile.getInputStream(zipEntries[page]));
                    Image im = image.getScaledInstance((int) ((int) image.getWidth() * renderSize), (int) ((int) image.getHeight() * renderSize), Image.SCALE_DEFAULT);
                    image = new BufferedImage(im.getWidth(null), im.getHeight(null), BufferedImage.TYPE_INT_RGB);
                    Graphics bg = image.getGraphics();
                    bg.drawImage(im, 0, 0, null);
                    bg.dispose();
                }

                if (tbNightMode.isSelected()) {
                    image = invertColors(image);
                }
                if (renderRotationAngle != 0) {
                    image = rotateImage(image, renderRotationAngle);
                }
                lblLastPage.setText(String.valueOf(documentSize));
                ImageIcon icon = new ImageIcon(image);
                pageRenderedImage[0].setIcon(icon);
                setCurrentPage(page);
                setMiniatures(currentPage);
                updateMiniaturesVerticalScrollBar();
                scrollPanePage.getVerticalScrollBar().setValue(0);
                scrollPanePage.requestFocus();
            }
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }

    private void loadPage(int page, int page1) {
        try {
            ImageIcon icon;
            final int lastPage = documentSize - 1;
            if (page == lastPage) {
                if (readMode.equals("pdf")) {
                    image = pdfRenderer.renderImage(page, renderSize);
                } else if (readMode.equals("zip")) {
                    image = ImageIO.read(zipFile.getInputStream(zipEntries[page]));
                    Image im = image.getScaledInstance((int) ((int) image.getWidth() * renderSize), (int) ((int) image.getHeight() * renderSize), Image.SCALE_DEFAULT);
                    image = new BufferedImage(im.getWidth(null), im.getHeight(null), BufferedImage.TYPE_INT_RGB);
                    Graphics bg = image.getGraphics();
                    bg.drawImage(im, 0, 0, null);
                    bg.dispose();
                }
                if (tbNightMode.isSelected()) {
                    image = invertColors(image);
                }
                if (renderRotationAngle != 0) {
                    image = rotateImage(image, renderRotationAngle);
                }
                lblLastPage.setText(String.valueOf(documentSize));
                icon = new ImageIcon(image);
                pageRenderedImage[0].setIcon(icon);
                pageRenderedImage[1].setIcon(null);
            } else {
                switch (page) {
                    case -1:
                        pageRenderedImage[0].setIcon(null);
                        break;
                    case 0:
                        pageRenderedImage[0].setIcon(null);
                        break;
                    default:
                        if (page > 0 && page < documentSize) {
                            if (readMode.equals("pdf")) {
                                image = pdfRenderer.renderImage(page, renderSize);
                            } else if (readMode.equals("zip")) {
                                image = ImageIO.read(zipFile.getInputStream(zipEntries[page]));
                                Image im = image.getScaledInstance((int) ((int) image.getWidth() * renderSize), (int) ((int) image.getHeight() * renderSize), Image.SCALE_DEFAULT);
                                image = new BufferedImage(im.getWidth(null), im.getHeight(null), BufferedImage.TYPE_INT_RGB);
                                Graphics bg = image.getGraphics();
                                bg.drawImage(im, 0, 0, null);
                                bg.dispose();
                            }
                            if (tbNightMode.isSelected()) {
                                image = invertColors(image);
                            }
                            if (renderRotationAngle != 0) {
                                image = rotateImage(image, renderRotationAngle);
                            }
                            lblLastPage.setText(String.valueOf(documentSize));
                            icon = new ImageIcon(image);
                            pageRenderedImage[0].setIcon(icon);
                        }
                        break;
                }
            }
            switch (page1) {
                case -1:
                    pageRenderedImage[1].setIcon(null);
                    break;
                case -5:
                    if (readMode.equals("pdf")) {
                        image = pdfRenderer.renderImage(page, renderSize);
                    } else if (readMode.equals("zip")) {
                        image = ImageIO.read(zipFile.getInputStream(zipEntries[page]));
                        Image im = image.getScaledInstance((int) ((int) image.getWidth() * renderSize), (int) ((int) image.getHeight() * renderSize), Image.SCALE_DEFAULT);
                        image = new BufferedImage(im.getWidth(null), im.getHeight(null), BufferedImage.TYPE_INT_RGB);
                        Graphics bg = image.getGraphics();
                        bg.drawImage(im, 0, 0, null);
                        bg.dispose();
                    }
                    if (tbNightMode.isSelected()) {
                        image = invertColors(image);
                    }
                    if (renderRotationAngle != 0) {
                        image = rotateImage(image, renderRotationAngle);
                    }
                    lblLastPage.setText(String.valueOf(documentSize));
                    icon = new ImageIcon(image);
                    pageRenderedImage[1].setIcon(icon);
                default:
                    if (page1 >= 0 && page1 < documentSize) {
                        if (readMode.equals("pdf")) {
                            image = pdfRenderer.renderImage(page1, renderSize);
                        } else if (readMode.equals("zip")) {
                            image = ImageIO.read(zipFile.getInputStream(zipEntries[page1]));
                            Image im = image.getScaledInstance((int) ((int) image.getWidth() * renderSize), (int) ((int) image.getHeight() * renderSize), Image.SCALE_DEFAULT);
                            image = new BufferedImage(im.getWidth(null), im.getHeight(null), BufferedImage.TYPE_INT_RGB);
                            Graphics bg = image.getGraphics();
                            bg.drawImage(im, 0, 0, null);
                            bg.dispose();
                        }
                        if (tbNightMode.isSelected()) {
                            image = invertColors(image);
                        }
                        if (renderRotationAngle != 0) {
                            image = rotateImage(image, renderRotationAngle);
                        }
                        lblLastPage.setText(String.valueOf(documentSize));
                        icon = new ImageIcon(image);
                        pageRenderedImage[1].setIcon(icon);
                    }
                    break;
            }
            if (page > 0) {
                setCurrentPage(page);
            } else {
                setCurrentPage(0);
            }
            setMiniatures(currentPage);
            scrollPanePage.getVerticalScrollBar().setValue(0);
            updateMiniaturesVerticalScrollBar();
            scrollPanePage.requestFocus();
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }

    private BufferedImage invertColors(BufferedImage image) {

        for (int i = 0; i < image.getHeight(); i++) {
            for (int j = 0; j < image.getWidth(); j++) {
                Color color = new Color(image.getRGB(j, i));
                if ((color.getRed() == 255) && (color.getRed() == 255) && (color.getRed() == 255)) {
                    image.setRGB(j, i, Color.BLACK.getRGB());
                }
                if ((color.getRed() == 0) && (color.getRed() == 0) && (color.getRed() == 0)) {
                    image.setRGB(j, i, Color.WHITE.getRGB());
                } else {
                    int red = 255 - color.getRed();
                    int blue = 255 - color.getBlue();
                    int green = 255 - color.getGreen();
                    color = new Color(red, green, blue);
                    image.setRGB(j, i, color.getRGB());
                }
            }
        }
        return image;
    }

    private void updateMiniaturesVerticalScrollBar() {
        int maxSize = scrollPaneMiniatures.getVerticalScrollBar().getMaximum();
        scrollPaneMiniatures.getVerticalScrollBar().setValue((maxSize / documentSize) * (currentPage - 1));
    }

    private void setMiniaturesGrid() {
        panelMiniatures.removeAll();
        if (readMode.equals("pdf")) {
            panelMiniatures.setLayout(new GridLayout(documentSize, 1, 0, 10));
        } else if (readMode.equals("zip")) {
            panelMiniatures.setLayout(new GridLayout(zipFile.size(), 1, 0, 10));
        }

        if (hasLoadedDocument) {
            for (JButton button : buttons) {
                button.removeAll();
            }
            hasLoadedDocument = false;
        }
    }

    private void setMiniatures(int page) {
        if (!hasLoadedDocument) {
            if (readMode.equals("pdf")) {
                buttons = new JButton[documentSize];
            } else if (readMode.equals("zip")) {
                buttons = new JButton[zipFile.size()];
            }

            for (int i = 0; i < buttons.length; i++) {
                buttons[i] = new JButton();
                buttons[i].setVisible(true);
                buttons[i].setBorderPainted(true);
                buttons[i].setContentAreaFilled(false);
                buttons[i].setToolTipText(String.valueOf(i + 1));
                buttons[i].setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
                panelMiniatures.add(buttons[i]);
                final int alfa = i;
                buttons[i].addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e
                    ) {
                        if (e.getButton() == MouseEvent.BUTTON1) {

                            if (BookMode == false) {
                                loadPage(alfa);
                            } else {
                                if (alfa % 2 != 0) {
                                    // is odd
                                    loadPage(alfa, alfa + 1);
                                } else {
                                    // is even
                                    loadPage(alfa - 1, alfa);
                                }
                            }
                            setMiniatures(currentPage);
                        }
                    }
                });
            }
        }
        int pageLoadMin = page - 3;
        int pageLoadMax = page + 3;
        for (int i = 0; i < buttons.length; i++) {
            try {
                if ((i >= pageLoadMin) && (i <= pageLoadMax)) {
                    if (isLoadingDocument) {
                        if (readMode.equals("pdf")) {
                            image = pdfRenderer.renderImage(i, (float) 0.20);
                        } else if (readMode.equals("zip")) {
                            image = ImageIO.read(zipFile.getInputStream(zipEntries[i]));
                            Image im = image.getScaledInstance(122, 158, Image.SCALE_DEFAULT);
                            image = new BufferedImage(im.getWidth(null), im.getHeight(null), BufferedImage.TYPE_INT_RGB);
                            Graphics bg = image.getGraphics();
                            bg.drawImage(im, 0, 0, null);
                            bg.dispose();
                        }

                        if (tbNightMode.isSelected()) {
                            image = invertColors(image);
                        }
                        ImageIcon icon = new ImageIcon(image);
                        buttons[i].setIcon(icon);
                        miniatureHeight = icon.getIconHeight();
                        miniatureWidth = icon.getIconWidth();
                    } else {
                        if (buttons[i].getIcon() == whiteIcon) {
                            if (readMode.equals("pdf")) {
                                image = pdfRenderer.renderImage(i, (float) 0.20);

                            } else if (readMode.equals("zip")) {
                                image = ImageIO.read(zipFile.getInputStream(zipEntries[i]));
                                Image im = image.getScaledInstance(122, 158, Image.SCALE_DEFAULT);
                                image = new BufferedImage(im.getWidth(null), im.getHeight(null), BufferedImage.TYPE_INT_RGB);
                                Graphics bg = image.getGraphics();
                                bg.drawImage(im, 0, 0, null);
                                bg.dispose();
                            }

                            if (tbNightMode.isSelected()) {
                                image = invertColors(image);
                            }
                            ImageIcon icon = new ImageIcon(image);
                            buttons[i].setIcon(icon);
                            miniatureHeight = icon.getIconHeight();
                            miniatureWidth = icon.getIconWidth();
                        }
                    }
                } else {
                    if (miniatureHeight != -1) {
                        if (!hasLoadedDocument) {
                            image = new BufferedImage(miniatureWidth, miniatureHeight, BufferedImage.TYPE_INT_RGB);
                            Graphics2D graphics = image.createGraphics();
                            graphics.setPaint(new Color(255, 255, 255));
                            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
                            graphics.dispose();
                            whiteIcon = new ImageIcon(image);
                        }
                        buttons[i].setIcon(whiteIcon);
                    } else {
                        buttons[i].setIcon(null);
                    }
                }

            } catch (IOException ex) {
                logger.log(Level.SEVERE, null, ex);
            }

        }
        ;
        scrollPaneMiniatures.getVerticalScrollBar().setValue(scrollPaneMiniatures.getVerticalScrollBar().getValue() + 30);
        scrollPaneMiniatures.getVerticalScrollBar().setValue(scrollPaneMiniatures.getVerticalScrollBar().getValue() - 30);
    }

    private void setCurrentPage(int page) {
        currentPage = page + 1;
        if (currentPage <= 0) {
            currentPage = 1;
        }
        txtPage.setText(String.valueOf(currentPage));
    }

    private void BookMode() {
        renderSize = (float) 0.75;
        try {
            if (BookMode != tbBookMode.isSelected() && documentSize > 1) {
                if (tbBookMode.isSelected() == true) {
                    if (currentPage - 1 == 0) {
                        setupPageViewGrid(-1, currentPage - 1);
                        loadPage(-1, currentPage - 1);
                    } else {
                        if (currentPage % 2 == 0) {
                            // if page number is even
                            setupPageViewGrid(currentPage - 1, currentPage);
                            loadPage(currentPage - 1, currentPage);
                        } else {
                            // if page number is odd
                            setupPageViewGrid(currentPage - 2, currentPage - 1);
                            loadPage(currentPage - 2, currentPage - 1);
                        }
                    }
                } else {
                    setupPageViewGrid(currentPage - 1);
                    loadPage(currentPage - 1);
                }
            }
            BookMode = tbBookMode.isSelected();
            scrollPanePage.getVerticalScrollBar().setValue(scrollPanePage.getVerticalScrollBar().getValue() + 30);
            scrollPanePage.getVerticalScrollBar().setValue(scrollPanePage.getVerticalScrollBar().getValue() - 30);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Book mode error: ", e);
        }
    }

    private void TogglePanel() {
        if (tbTogglePanel.isSelected()) {
            jTabbedPane1.setVisible(true);
            scrollPanePage.setSize((scrollPanePage.getWidth() - leftPaneWidth), scrollPanePage.getHeight());
            panelBackground.updateUI();

        } else {
            jTabbedPane1.setVisible(false);
            scrollPanePage.setSize((scrollPanePage.getWidth() + leftPaneWidth), scrollPanePage.getHeight());
            panelBackground.updateUI();
        }
    }

    private void zoomIn() {
        if (renderSize < 2) {
            renderSize = renderSize + (float) 0.25;
            if (BookMode == false) {
                loadPage(currentPage - 1);
            } else {
                if (currentPage == 1) {
                    loadPage(- 1, 0);
                } else {
                    loadPage(currentPage - 1, currentPage);
                }
            }
        }
    }

    private void zoomOut() {
        if (renderSize >= 0.5) {
            renderSize = renderSize - (float) 0.25;
            if (BookMode == false) {
                loadPage(currentPage - 1);
            } else {
                if (currentPage == 1) {
                    loadPage(- 1, 0);
                } else {
                    loadPage(currentPage - 1, currentPage);
                }
            }
        }
    }

    private void nextPage() {

        if (hasLoadedDocument) {
            if (BookMode == false) {
                if (currentPage + 1 <= documentSize) {
                    loadPage(currentPage);
                }
            } else {
                if (currentPage + 2 == documentSize) {
                    if (documentSize == 3) {
                        loadPage(currentPage, currentPage + 1);
                    } else {
                        loadPage(currentPage + 2, -1);
                    }
                } else if (currentPage % 2 != 0) {
                    // is odd
                    if (currentPage + 1 < documentSize) {
                        System.out.println("hey");
                        loadPage(currentPage, currentPage + 1);
                    }
                } else {
                    // is even
                    if (currentPage + 2 < documentSize) {
                        System.out.println("ho");
                        loadPage(currentPage + 1, currentPage + 2);
                    }
                }
            }
        }
    }

    private void previousPage() {
        if (hasLoadedDocument) {
            if (BookMode == false) {
                if (currentPage - 2 >= 0) {
                    loadPage(currentPage - 2);
                }
            } else {
                if (currentPage == 2) {
                    //  show first page on right side
                    loadPage(-1, currentPage - 2);
                } else if (currentPage % 2 != 0) {
                    if (currentPage > 2) {
                        loadPage(currentPage - 3, currentPage - 2);
                    } else {
                        loadPage(currentPage - 4, currentPage - 3);
                    }
                } else {
                    if (currentPage > 2) {
                        loadPage(currentPage - 3, currentPage - 2);
                    } else {
                        loadPage(currentPage - 4, currentPage - 3);
                    }
                }
            }
        }
    }

    private void firstPage() {
        if (hasLoadedDocument) {
            if (BookMode == false) {
                loadPage(0);
            } else {
                loadPage(-1, 0);
            }
        }
    }

    private void lastPage() {
        if (hasLoadedDocument) {
            if (BookMode == false) {
                loadPage(documentSize - 1);
            } else {
                if ((documentSize) % 2 == 0) {
                    //if last page is even
                    loadPage(documentSize - 1, -1);
                } else {
                    // if last page is odd
                    loadPage(documentSize - 2, documentSize - 1);
                }
            }
        }
    }

    private void printDocument() {
        if (hasLoadedDocument) {
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setPageable(new PDFPageable(document));

            if (job.printDialog()) {
                try {
                    job.print();
                } catch (PrinterException ex) {
                    logger.log(Level.SEVERE, "Print error: ", ex);
                }
            }
        }
    }

    private void openDocument() {
        final JFileChooser openFile = new JFileChooser();
        openFile.setMultiSelectionEnabled(false);
        int actionDialog = openFile.showOpenDialog(this);
        if (actionDialog == JFileChooser.APPROVE_OPTION) {
            String alfa = openFile.getSelectedFile().getAbsolutePath();
            Object o = SwingUtilities.windowForComponent(this);
            if (o != null && o instanceof JFrame) {
                JFrame frame = (JFrame) o;
                frame.setTitle("Tantan Reader - " + openFile.getSelectedFile().getName());
            }
            if (alfa.endsWith(".pdf")) {
                if (hasLoadedDocument) {
                    try {
                        document.close();
                    } catch (IOException ex) {
                        logger.log(Level.WARNING, "Error closing document: " + ex.toString(), ex);
                    }
                }
                readMode = "pdf";
                loadFile(alfa);
            } else if (alfa.endsWith(".cbz") || alfa.endsWith(".zip")) {
                readMode = "zip";
                loadFile(alfa);
            } else {
                java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("com/github/tantanreader/bundles/Bundle");
                JOptionPane.showMessageDialog(PDFReader.this, bundle.getString("Main.Message.FileNotSupported"));
            }
        }
    }

    public void closeDocument() {
        try {
            document.close();
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }

    private BufferedImage rotateImage(BufferedImage imageToRotate, int angleToRotate) {
        System.out.println("rotate");

        int width = imageToRotate.getWidth();
        int height = imageToRotate.getHeight();
        double angle = 0;
        BufferedImage dest = new BufferedImage(height, width, imageToRotate.getType());
        Graphics2D graphics2D = dest.createGraphics();
        switch (angleToRotate) {
            case 90:
                angle = Math.PI / 2;

                graphics2D.translate(90, 90);
                graphics2D.rotate(angle, height / 2, width / 2);
                graphics2D.drawRenderedImage(imageToRotate, null);
                break;
            case 180:
                angle = Math.PI;

                graphics2D.translate(-90, -90);
                graphics2D.rotate(angle, height / 2, width / 2);
                graphics2D.drawRenderedImage(imageToRotate, null);
                break;
            case 270:
                angle = (3 * Math.PI) / 2;
                break;
        }

        return dest;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        panelBackground = new javax.swing.JPanel();
        toolbarPanel = new javax.swing.JPanel();
        jToolBar2 = new javax.swing.JToolBar();
        btnOpen = new javax.swing.JButton();
        btnPrint = new javax.swing.JButton();
        tbTogglePanel = new javax.swing.JToggleButton();
        tbNightMode = new javax.swing.JToggleButton();
        tbBookMode = new javax.swing.JToggleButton();
        tbFullscreen = new javax.swing.JToggleButton();
        btnZoomOut = new javax.swing.JButton();
        btnZoomIn = new javax.swing.JButton();
        jToolBar1 = new javax.swing.JToolBar();
        btnFirstPage = new javax.swing.JButton();
        btnPreviousPage = new javax.swing.JButton();
        txtPage = new javax.swing.JFormattedTextField();
        lblPageSeparator = new javax.swing.JLabel();
        lblLastPage = new javax.swing.JLabel();
        btnNextPage = new javax.swing.JButton();
        btnLastPage = new javax.swing.JButton();
        jToolBar4 = new javax.swing.JToolBar();
        btnInfo = new javax.swing.JButton();
        scrollPanePage = new javax.swing.JScrollPane();
        panel = new javax.swing.JPanel();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        scrollPaneMiniatures = new javax.swing.JScrollPane();
        panelMiniatures = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTree1 = new javax.swing.JTree();

        setLayout(new java.awt.BorderLayout());

        panelBackground.setBackground(new java.awt.Color(51, 51, 51));

        toolbarPanel.setBackground(new java.awt.Color(51, 51, 51));

        jToolBar2.setBackground(new java.awt.Color(51, 51, 51));
        jToolBar2.setBorder(null);
        jToolBar2.setFloatable(false);
        jToolBar2.setRollover(true);
        jToolBar2.setBorderPainted(false);

        btnOpen.setBackground(new java.awt.Color(51, 51, 51));
        btnOpen.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/github/tantanreader/icons/open.png"))); // NOI18N
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("com/github/tantanreader/bundles/Bundle"); // NOI18N
        btnOpen.setToolTipText(bundle.getString("Main.btnOpen.tooltip")); // NOI18N
        btnOpen.setFocusable(false);
        btnOpen.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnOpen.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnOpen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnOpenActionPerformed(evt);
            }
        });
        jToolBar2.add(btnOpen);

        btnPrint.setBackground(new java.awt.Color(51, 51, 51));
        btnPrint.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/github/tantanreader/icons/print.png"))); // NOI18N
        btnPrint.setToolTipText(bundle.getString("Main.btnPrint.tooltip")); // NOI18N
        btnPrint.setFocusable(false);
        btnPrint.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnPrint.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnPrint.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPrintActionPerformed(evt);
            }
        });
        jToolBar2.add(btnPrint);

        tbTogglePanel.setBackground(new java.awt.Color(51, 51, 51));
        tbTogglePanel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/github/tantanreader/icons/panel.png"))); // NOI18N
        tbTogglePanel.setSelected(true);
        tbTogglePanel.setToolTipText(bundle.getString("Main.btPanel.tooltip")); // NOI18N
        tbTogglePanel.setFocusable(false);
        tbTogglePanel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        tbTogglePanel.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        tbTogglePanel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tbTogglePanelActionPerformed(evt);
            }
        });
        jToolBar2.add(tbTogglePanel);

        tbNightMode.setBackground(new java.awt.Color(51, 51, 51));
        tbNightMode.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/github/tantanreader/icons/nightmode.png"))); // NOI18N
        tbNightMode.setToolTipText(bundle.getString("Main.tbNightMode.toolTipText")); // NOI18N
        tbNightMode.setFocusable(false);
        tbNightMode.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        tbNightMode.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        tbNightMode.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tbNightModeActionPerformed(evt);
            }
        });
        jToolBar2.add(tbNightMode);

        tbBookMode.setBackground(new java.awt.Color(51, 51, 51));
        tbBookMode.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/github/tantanreader/icons/bookmode.png"))); // NOI18N
        tbBookMode.setToolTipText(bundle.getString("Main.btnBookMode.tooltip")); // NOI18N
        tbBookMode.setFocusable(false);
        tbBookMode.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        tbBookMode.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        tbBookMode.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tbBookModeActionPerformed(evt);
            }
        });
        jToolBar2.add(tbBookMode);

        tbFullscreen.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/github/tantanreader/icons/fullscreen.png"))); // NOI18N
        tbFullscreen.setToolTipText(bundle.getString("Main.tbFullscreen.tooltip")); // NOI18N
        tbFullscreen.setFocusable(false);
        tbFullscreen.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        tbFullscreen.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        tbFullscreen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tbFullscreenActionPerformed(evt);
            }
        });
        jToolBar2.add(tbFullscreen);

        btnZoomOut.setBackground(new java.awt.Color(51, 51, 51));
        btnZoomOut.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        btnZoomOut.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/github/tantanreader/icons/zoomOut.png"))); // NOI18N
        btnZoomOut.setToolTipText(bundle.getString("Main.btnZoomOut.tooltip")); // NOI18N
        btnZoomOut.setFocusable(false);
        btnZoomOut.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnZoomOut.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnZoomOut.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnZoomOutActionPerformed(evt);
            }
        });
        jToolBar2.add(btnZoomOut);

        btnZoomIn.setBackground(new java.awt.Color(51, 51, 51));
        btnZoomIn.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        btnZoomIn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/github/tantanreader/icons/zoomIn.png"))); // NOI18N
        btnZoomIn.setToolTipText(bundle.getString("Main.btnZoomIn.tooltip")); // NOI18N
        btnZoomIn.setFocusable(false);
        btnZoomIn.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnZoomIn.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnZoomIn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnZoomInActionPerformed(evt);
            }
        });
        jToolBar2.add(btnZoomIn);

        jToolBar1.setBackground(new java.awt.Color(51, 51, 51));
        jToolBar1.setBorder(null);
        jToolBar1.setFloatable(false);
        jToolBar1.setRollover(true);
        jToolBar1.setBorderPainted(false);
        jToolBar1.setMaximumSize(new java.awt.Dimension(517, 25));
        jToolBar1.setMinimumSize(new java.awt.Dimension(505, 25));

        btnFirstPage.setBackground(new java.awt.Color(51, 51, 51));
        btnFirstPage.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/github/tantanreader/icons/firstPage.png"))); // NOI18N
        btnFirstPage.setToolTipText(bundle.getString("Main.btnFirstPage.tooltip")); // NOI18N
        btnFirstPage.setFocusable(false);
        btnFirstPage.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnFirstPage.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnFirstPage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnFirstPageActionPerformed(evt);
            }
        });
        jToolBar1.add(btnFirstPage);

        btnPreviousPage.setBackground(new java.awt.Color(51, 51, 51));
        btnPreviousPage.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/github/tantanreader/icons/previousPage.png"))); // NOI18N
        btnPreviousPage.setToolTipText(bundle.getString("Main.btnPreviousPage.tooltip")); // NOI18N
        btnPreviousPage.setFocusable(false);
        btnPreviousPage.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnPreviousPage.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnPreviousPage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPreviousPageActionPerformed(evt);
            }
        });
        jToolBar1.add(btnPreviousPage);

        txtPage.setBackground(new java.awt.Color(102, 102, 102));
        txtPage.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        txtPage.setForeground(new java.awt.Color(204, 204, 204));
        txtPage.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        txtPage.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        txtPage.setMaximumSize(new java.awt.Dimension(50, 100));
        txtPage.setMinimumSize(new java.awt.Dimension(50, 800));
        txtPage.setPreferredSize(new java.awt.Dimension(50, 23));
        txtPage.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                txtPageKeyReleased(evt);
            }
        });
        jToolBar1.add(txtPage);

        lblPageSeparator.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        lblPageSeparator.setForeground(new java.awt.Color(204, 204, 204));
        lblPageSeparator.setText(" / "); // NOI18N
        jToolBar1.add(lblPageSeparator);

        lblLastPage.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        lblLastPage.setForeground(new java.awt.Color(204, 204, 204));
        lblLastPage.setText("1000"); // NOI18N
        jToolBar1.add(lblLastPage);

        btnNextPage.setBackground(new java.awt.Color(51, 51, 51));
        btnNextPage.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/github/tantanreader/icons/nextPage.png"))); // NOI18N
        btnNextPage.setToolTipText(bundle.getString("Main.btnNextPage.tooltip")); // NOI18N
        btnNextPage.setFocusable(false);
        btnNextPage.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnNextPage.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnNextPage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnNextPageActionPerformed(evt);
            }
        });
        jToolBar1.add(btnNextPage);

        btnLastPage.setBackground(new java.awt.Color(51, 51, 51));
        btnLastPage.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/github/tantanreader/icons/lastPage.png"))); // NOI18N
        btnLastPage.setToolTipText(bundle.getString("Main.btnLastPage.tooltip")); // NOI18N
        btnLastPage.setFocusable(false);
        btnLastPage.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnLastPage.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnLastPage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnLastPageActionPerformed(evt);
            }
        });
        jToolBar1.add(btnLastPage);

        jToolBar4.setBackground(new java.awt.Color(51, 51, 51));
        jToolBar4.setBorder(null);
        jToolBar4.setFloatable(false);
        jToolBar4.setRollover(true);
        jToolBar4.setAlignmentY(0.5F);
        jToolBar4.setBorderPainted(false);

        btnInfo.setBackground(new java.awt.Color(51, 51, 51));
        btnInfo.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/github/tantanreader/icons/info.png"))); // NOI18N
        btnInfo.setToolTipText(bundle.getString("Main.btnInfo.tooltip")); // NOI18N
        btnInfo.setFocusable(false);
        btnInfo.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnInfo.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnInfo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnInfoActionPerformed(evt);
            }
        });
        jToolBar4.add(btnInfo);

        javax.swing.GroupLayout toolbarPanelLayout = new javax.swing.GroupLayout(toolbarPanel);
        toolbarPanel.setLayout(toolbarPanelLayout);
        toolbarPanelLayout.setHorizontalGroup(
            toolbarPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(toolbarPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jToolBar2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jToolBar1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jToolBar4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        toolbarPanelLayout.setVerticalGroup(
            toolbarPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(toolbarPanelLayout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(toolbarPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jToolBar1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jToolBar4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jToolBar2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 0, 0))
        );

        scrollPanePage.setBackground(new java.awt.Color(51, 51, 51));
        scrollPanePage.setBorder(null);
        scrollPanePage.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                scrollPanePageMouseClicked(evt);
            }
        });
        scrollPanePage.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                scrollPanePageKeyPressed(evt);
            }
            public void keyReleased(java.awt.event.KeyEvent evt) {
                scrollPanePageKeyReleased(evt);
            }
        });

        panel.setBackground(new java.awt.Color(51, 51, 51));
        panel.setForeground(new java.awt.Color(51, 51, 51));
        panel.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                panelKeyReleased(evt);
            }
        });
        panel.setLayout(new java.awt.GridBagLayout());
        scrollPanePage.setViewportView(panel);

        jTabbedPane1.setBackground(new java.awt.Color(51, 51, 51));
        jTabbedPane1.setForeground(new java.awt.Color(240, 240, 240));
        jTabbedPane1.setPreferredSize(new java.awt.Dimension(180, 556));

        scrollPaneMiniatures.setBorder(null);
        scrollPaneMiniatures.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        panelMiniatures.setBackground(new java.awt.Color(51, 51, 51));
        panelMiniatures.setMaximumSize(new java.awt.Dimension(32767, 200));

        javax.swing.GroupLayout panelMiniaturesLayout = new javax.swing.GroupLayout(panelMiniatures);
        panelMiniatures.setLayout(panelMiniaturesLayout);
        panelMiniaturesLayout.setHorizontalGroup(
            panelMiniaturesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 169, Short.MAX_VALUE)
        );
        panelMiniaturesLayout.setVerticalGroup(
            panelMiniaturesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 528, Short.MAX_VALUE)
        );

        scrollPaneMiniatures.setViewportView(panelMiniatures);

        jTabbedPane1.addTab(bundle.getString("PDFReader.scrollPaneMiniatures.TabConstraints.tabTitle"), scrollPaneMiniatures); // NOI18N

        jTree1.setBackground(new java.awt.Color(51, 51, 51));
        jTree1.setForeground(new java.awt.Color(255, 255, 255));
        jTree1.setName(""); // NOI18N
        jTree1.setRootVisible(false);
        jScrollPane1.setViewportView(jTree1);

        jTabbedPane1.addTab(bundle.getString("PDFReader.jScrollPane1.TabConstraints.tabTitle"), jScrollPane1); // NOI18N

        javax.swing.GroupLayout panelBackgroundLayout = new javax.swing.GroupLayout(panelBackground);
        panelBackground.setLayout(panelBackgroundLayout);
        panelBackgroundLayout.setHorizontalGroup(
            panelBackgroundLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelBackgroundLayout.createSequentialGroup()
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(scrollPanePage))
            .addComponent(toolbarPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        panelBackgroundLayout.setVerticalGroup(
            panelBackgroundLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelBackgroundLayout.createSequentialGroup()
                .addComponent(toolbarPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGroup(panelBackgroundLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(scrollPanePage, javax.swing.GroupLayout.DEFAULT_SIZE, 370, Short.MAX_VALUE)
                    .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)))
        );

        add(panelBackground, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    private void btnOpenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnOpenActionPerformed
        openDocument();
    }//GEN-LAST:event_btnOpenActionPerformed

    private void btnPrintActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPrintActionPerformed
        printDocument();
    }//GEN-LAST:event_btnPrintActionPerformed

    private void tbTogglePanelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tbTogglePanelActionPerformed

        TogglePanel();
    }//GEN-LAST:event_tbTogglePanelActionPerformed

    private void tbNightModeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tbNightModeActionPerformed
        if (hasLoadedDocument) {
            if (BookMode == false) {
                loadPage(currentPage - 1);
            } else {
                loadPage(currentPage - 1, currentPage);
            }
            updateMiniaturesVerticalScrollBar();
            setMiniatures(currentPage);
        }
    }//GEN-LAST:event_tbNightModeActionPerformed

    private void tbBookModeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tbBookModeActionPerformed
        if (hasLoadedDocument) {
            if (documentSize > 1) {
                BookMode();
            } else {
                tbBookMode.setSelected(false);
            }
        }
    }//GEN-LAST:event_tbBookModeActionPerformed

    private void btnZoomOutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnZoomOutActionPerformed
        if (hasLoadedDocument) {
            zoomOut();
        }
    }//GEN-LAST:event_btnZoomOutActionPerformed

    private void btnZoomInActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnZoomInActionPerformed
        if (hasLoadedDocument) {
            zoomIn();
        }
    }//GEN-LAST:event_btnZoomInActionPerformed

    private void btnFirstPageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnFirstPageActionPerformed
        firstPage();
    }//GEN-LAST:event_btnFirstPageActionPerformed

    private void btnPreviousPageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPreviousPageActionPerformed
        previousPage();
    }//GEN-LAST:event_btnPreviousPageActionPerformed

    private void txtPageKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtPageKeyReleased
        if (hasLoadedDocument) {
            if ("10".equals(String.valueOf(evt.getKeyCode()))) { //event code for enter key
                try {
                    int i = Integer.parseInt(txtPage.getText().replace(" ", ""));
                    if (BookMode == false) {
                        loadPage(i - 1);
                    } else {
                        if (i % 2 != 0) {
                            // is odd
                            loadPage(i - 2, i - 1);
                        } else {
                            // is even
                            loadPage(i - 1, i);
                        }
                    }
                } catch (Exception ex) {
                    System.out.println(ex);
                }
            }
        }
    }//GEN-LAST:event_txtPageKeyReleased

    private void btnNextPageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnNextPageActionPerformed
        nextPage();
    }//GEN-LAST:event_btnNextPageActionPerformed

    private void btnLastPageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLastPageActionPerformed
        lastPage();
    }//GEN-LAST:event_btnLastPageActionPerformed

    private void btnInfoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnInfoActionPerformed
        InfoFrame info = new InfoFrame();
        info.setVisible(true);
    }//GEN-LAST:event_btnInfoActionPerformed

    private void panelKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_panelKeyReleased

    }//GEN-LAST:event_panelKeyReleased

    private void scrollPanePageMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_scrollPanePageMouseClicked
        scrollPanePage.requestFocus();
    }//GEN-LAST:event_scrollPanePageMouseClicked

    private void scrollPanePageKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_scrollPanePageKeyPressed

        switch (evt.getKeyCode()) {
            case 17:
                // ctrl
                isCtrlPressed = true;
                break;
            case 80:
                // P
                if (isCtrlPressed) {
                    printDocument();
                }
                break;
            case 79:
                // o
                if (isCtrlPressed) {
                    openDocument();
                }
                break;
            default:
                isCtrlPressed = false;
                break;
        }
    }//GEN-LAST:event_scrollPanePageKeyPressed

    private void scrollPanePageKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_scrollPanePageKeyReleased

        switch (evt.getKeyCode()) {
            case 38:
                // up
                if (scrollPanePage.getVerticalScrollBar().getValue() == 0) {
                    previousPage();
                }
                break;
            case 40:
                // down
                int maxValue = scrollPanePage.getVerticalScrollBar().getMaximum();
                int value = scrollPanePage.getVerticalScrollBar().getValue();
                int extentValue = scrollPanePage.getVerticalScrollBar().getModel().getExtent();
                if (value + extentValue >= maxValue) {
                    nextPage();
                }
                break;
            case 99:
                // page down
                nextPage();
                break;
            case 105:
                // page up
                previousPage();
                break;
            case 103:
                // home
                firstPage();
                break;
            case 97:
                // end
                lastPage();
                break;

        }
    }//GEN-LAST:event_scrollPanePageKeyReleased

    private void tbFullscreenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tbFullscreenActionPerformed
        if (tbFullscreen.isSelected()) {
            Object o = SwingUtilities.windowForComponent(this);
            if (o != null && o instanceof JFrame) {
                JFrame frame = (JFrame) o;
                frame.dispose();
                frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
                frame.setUndecorated(true);
                frame.setVisible(true);
            }
        } else {
            Object o = SwingUtilities.windowForComponent(this);
            if (o != null && o instanceof JFrame) {
                JFrame frame = (JFrame) o;
                frame.dispose();
                frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
                frame.setUndecorated(false);
                frame.setVisible(true);
            }
        }
    }//GEN-LAST:event_tbFullscreenActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnFirstPage;
    private javax.swing.JButton btnInfo;
    private javax.swing.JButton btnLastPage;
    private javax.swing.JButton btnNextPage;
    private javax.swing.JButton btnOpen;
    private javax.swing.JButton btnPreviousPage;
    private javax.swing.JButton btnPrint;
    private javax.swing.JButton btnZoomIn;
    private javax.swing.JButton btnZoomOut;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JToolBar jToolBar1;
    private javax.swing.JToolBar jToolBar2;
    private javax.swing.JToolBar jToolBar4;
    private javax.swing.JTree jTree1;
    private javax.swing.JLabel lblLastPage;
    private javax.swing.JLabel lblPageSeparator;
    private javax.swing.JPanel panel;
    private javax.swing.JPanel panelBackground;
    private javax.swing.JPanel panelMiniatures;
    private javax.swing.JScrollPane scrollPaneMiniatures;
    private javax.swing.JScrollPane scrollPanePage;
    private javax.swing.JToggleButton tbBookMode;
    private javax.swing.JToggleButton tbFullscreen;
    private javax.swing.JToggleButton tbNightMode;
    private javax.swing.JToggleButton tbTogglePanel;
    private javax.swing.JPanel toolbarPanel;
    private javax.swing.JFormattedTextField txtPage;
    // End of variables declaration//GEN-END:variables
}
