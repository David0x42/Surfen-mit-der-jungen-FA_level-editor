import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.*;

public class MainClass  extends JPanel {

    public static Path pathToLevel;

    private static final long serialVersionUID = 1L;
    private static BufferedImage image;
    private static  BufferedImage imageErr;

    public static  ArrayList<BufferedImage> blocks = new ArrayList<BufferedImage>();


    public static JFrame f;
    public static JPanel canvas;


    public static ArrayList<Integer> bytes = new ArrayList<>();


    public static byte[] data = null;

    public static int i_toolBarLoop =0;
    public static int leftMouseSelectedByte =  0xAE;

    public static int zoomLevel = 1;  // only 1 or 2 supported. (1=no zoom, 2= images scaled by 2x)

    private static String frameTitle = "\"Surfen mit der jungen FA\" - level editor";

    public static void updateView() {




        canvas = null;

        canvas = new JPanel() {
            private static final long serialVersionUID = 1L;
            @Override
            protected void paintComponent(Graphics g) {

                super.paintComponent(g);

                repaint();

                // draw black lines left and right to the level
                g.fillRect(0,75*zoomLevel,10,19025 *zoomLevel);
                g.fillRect(10+(16*zoomLevel)*20,75*zoomLevel,10,19025 *zoomLevel);

                int c = 1; //count of lines drawen of the level
                int x = 20*(16*zoomLevel) -(16*zoomLevel); //x position to draw the next level block
                int y = 19100*zoomLevel - (16*zoomLevel); //y position to draw the next level block

                for(int b: bytes) {

                    //safety
                    if(b<blocks.size()) {
                        image = blocks.get((int)b);
                    } else {
                        image = imageErr;

                    }

                    g.drawImage(image.getScaledInstance(16*zoomLevel, 16*zoomLevel,
                            java.awt.Image.SCALE_SMOOTH), x+10, y, null);

                    c=c+1;
                    x = x -(16*zoomLevel);
                    if(c>20) {
                        c = 1;
                        y = y - (16*zoomLevel);
                        x = 20*(16*zoomLevel) -(16*zoomLevel);
                    } // end if (reached new line in the level)
                } //end for (parsed all bytes from the level file)
            } //end paintComponent function (finished drawing whole level)
        }; //end JPanel generation

    } //end of updateView function

    public static void setLevelPath(Path p) {
        pathToLevel = p;
    }
    public static void readLevelFile() {


        try {
            data = Files.readAllBytes(pathToLevel);

        } catch (Exception e)
        {
            System.err.println("Could not read file from path: "+pathToLevel);
            System.err.println(e);
            return;
        }

        bytes.clear();

        //read the file reverse (as the game does)
        for(int i = data.length-1; i>=0; i--){  //  i>=0  correct??  was i>0
            bytes.add(data[i] & 0xff);

        }
    }

    public MainClass() {


        setLevelPath(Paths.get("src/main/resources/LEVEL"));
        readLevelFile();


        //open "blocks" / ~256 little images the level consists of
        try {

            for(int i = 0; i< 256; i++) {

                String hexStr = Integer.toHexString(i).toUpperCase();

                // convert into two digit hex format  e.g. "00",  "0F", "A2", ...
                if(hexStr.equals("0")) {
                    hexStr = "00";
                } else if(hexStr.length() == 1){
                    hexStr = "0".concat(hexStr);
                }
                String filename ="src/main/resources/extracted-blocks/"+hexStr+".bmp";
                System.out.println("reading: "+filename);

                blocks.add(ImageIO.read(new File(filename)));
            }


            this.imageErr = ImageIO.read(new File("src/main/resources/extracted-blocks/err.bmp"));



        }catch(IOException ex) {
            System.err.println("error while reading default blocks images a level consists of");
            System.err.println(ex);
        } // end of reading blocks


        updateView();  // create Canvas

        //Create a file chooser
        final JFileChooser fc = new JFileChooser();
        JButton openFileButton = new JButton("Open LEVEL");

        openFileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //Handle open button action.
                if (e.getSource() == openFileButton) {
                    int returnVal = fc.showOpenDialog(MainClass.this);

                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        File file = fc.getSelectedFile();
                        setLevelPath(Paths.get(fc.getSelectedFile().getAbsolutePath()));
                        readLevelFile();
                        //This is where a real application would open the file.
                      System.err.println("Opening: " +  file.getAbsolutePath()+".");
                      frameTitle = "\"Surfen mit der jungen FA\" - level editor" + "   file opened ("+pathToLevel.toString()+")";
                      f.setTitle(frameTitle);

                    } else {
                        System.err.println("Open command cancelled by user.");
                    }
                }
            }
        });

        canvas.add(openFileButton);



        // create exit button
        JButton buttonExit = new JButton("return to DOS");
        buttonExit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
        canvas.add(buttonExit);



        //create zoom Button
        JButton buttonZoom = new JButton();
        if(zoomLevel == 2) {
            buttonZoom.setText("zoom out");
        } else  {
            buttonZoom.setText("zoom in");
        }

        buttonZoom.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                if(zoomLevel == 2) {
                    zoomLevel =1;
                } else  {
                    zoomLevel = 2;
                }
                String[] arr = {String.valueOf(zoomLevel)};
                f.dispose();
                main(arr); //restart with new zoom mode

            }
        });
        canvas.add(buttonZoom);


        //create save file button
        JButton buttonSave = new JButton("save Level");
        buttonSave.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("trying to save file...");

                try {
                    byte[] bytesToWrite =new byte[bytes.size()];

                    for(int i=bytes.size()-1; i >=0; i--) {
                        System.out.println("bytes.size(): " +bytes.size());
                        bytesToWrite[bytes.size()-1-i] = bytes.get(i).byteValue();
                    }

                    Files.write(pathToLevel, bytesToWrite);
                }catch (Exception fileEx) {
                    System.err.println("could not write file: "+pathToLevel);
                    System.err.println(fileEx);

                }
                System.out.println("file saved");
            }

        });
        canvas.add(buttonSave);


        canvas.setPreferredSize(new Dimension(360* zoomLevel, 19100*zoomLevel+50));


        canvas.addMouseListener(new MouseListener(){
            public void mouseClicked(MouseEvent e){
             //   System.out.println("Mouse Button: "+ e.getButton() +" was clicked");
             //   System.out.println("e.x: " +e.getX() +"  |  e.getY(): "+e.getY());

                //convert (X,Y) to by byte offset
            int x,y;

                x= 20-(e.getX()-10) / (16*zoomLevel);
                y = (1188-((e.getY()-93*zoomLevel) / (16*zoomLevel)));

                System.out.println(("x:"+x+"   y:"+y));

                // write new Data at the position in the bytes array where the user clicked at on the screen.
                if(e.getButton() == 1) //left mouse button
                  bytes.set(((y-1)*20+(x-1)), leftMouseSelectedByte);
                if(e.getButton() == 3) //right mouse button
                    bytes.set(((y-1)*20+(x-1)),0x7E);  //just one of the waters

                  updateView();
            }

            // these stubs are needed
            public void mouseExited(MouseEvent arg0) {}
            public void mouseEntered(MouseEvent arg0) {}
            public void mousePressed(MouseEvent arg0) {}
            public void mouseReleased(MouseEvent arg0) {}


        }); // end of Mouse button listener


        JScrollPane sp = new JScrollPane(canvas);
        sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sp.getVerticalScrollBar().setPreferredSize(new Dimension(25*zoomLevel,0));
        setLayout(new BorderLayout());
        add(sp, BorderLayout.WEST);





    }

    public static void main(String[] args) {

        if(args.length>0)
        zoomLevel = Integer.parseInt(args[0]);


        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                JPanel p = new MainClass();
                f = new JFrame();

                f.setContentPane(p);
                if(zoomLevel == 1) {
                    f.setSize(570 * zoomLevel, 1000);

                } else {
                    f.setSize(1330, 1000);
                }
                f.setResizable(false);

                frameTitle = "\"Surfen mit der jungen FA\" - level editor" + "   file opened ("+pathToLevel.toString()+")";
                f.setTitle(frameTitle);

                JPanel toolbar = new JPanel();
                toolbar.setSize(100,800);
                toolbar.setBorder(BorderFactory.createTitledBorder("Level Elements"));

                if(zoomLevel == 1) {
                    toolbar.setLayout(new GridLayout(32, 8, 10, 10)); // if zoomlevel = 1;
                }else {
                    toolbar.setLayout(new GridLayout(16, 16, 10, 10)); // if zoomlevel = 2;
                }


                BufferedImage toolbarTestImage;



                ArrayList<JButton> buttonList = new ArrayList<JButton>();
                for(int i = 0; i < 256 -32; i++) {


                    String hexStr = Integer.toHexString(i).toUpperCase();

                    // convert into two digit hex format  e.g. "00",  "0F", "A2", ...
                    if(hexStr.equals("0")) {
                        hexStr = "00";
                    } else if(hexStr.length() == 1){
                        hexStr = "0".concat(hexStr);
                    }
                    String filename ="src/main/resources/extracted-blocks/"+hexStr+".bmp";

                    try {
                        toolbarTestImage = ImageIO.read(new File(filename));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }


                    JButton tempButton = new JButton(new ImageIcon(((new ImageIcon(toolbarTestImage).getImage()
                            .getScaledInstance(16*zoomLevel, 16*zoomLevel,
                                    java.awt.Image.SCALE_SMOOTH)))));

                    final int buttonImageIndex = i;
                    tempButton.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            leftMouseSelectedByte = buttonImageIndex;

                        }
                    });

                    tempButton.setPreferredSize(new Dimension(16*zoomLevel,16*zoomLevel));
                    tempButton.setMaximumSize(new Dimension(10,10));
                    buttonList.add(tempButton);


                    toolbar.add(tempButton);

                }

                f.getContentPane().add(toolbar, BorderLayout.EAST);

                f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                f.setVisible(true);

            }
        });

    }
}