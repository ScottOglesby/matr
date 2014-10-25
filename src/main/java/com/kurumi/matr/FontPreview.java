package com.kurumi.matr;

import java.awt.*;

import javax.swing.JFrame;
/**
 * Font previewer: lets user choose font sizes for display.
 * Currently not enabled -- assumes all fonts are Helvetica
 * @author soglesby
 *
 */
public class FontPreview {
   private TextField tfrouteLogFont = 
      new TextField(""+Empire.routeLogFont.getSize(), 3);
   private TextField tftownFont = 
      new TextField(""+Empire.townFont.getSize(), 3);
   private TextField tfmedRouteFont = 
      new TextField(""+MapUtils.getFont(MapUtils.medRoute).getSize(), 3);
   private TextField tfsmallRouteFont =
      new TextField(""+MapUtils.getFont(MapUtils.smallRoute).getSize(), 3);
   private TextField tfdirTabFont = 
      new TextField(""+Empire.dirTabFont.getSize(), 3);
   private TextField tfrouteTabFont = 
      new TextField(""+Empire.routeTabFont.getSize(), 3);
   private TextField tfstreetTabFont = 
      new TextField(""+SignUtils.getFont().getSize(), 3);

   private TextField tfLogSample = new TextField("Log Text", 10);

   private PMapSample map = new PMapSample();
   private PSignSample sign = new PSignSample();

   private Button bApply = new Button("Apply");
   private Button bClose = new Button("Close");

   private MatrPanel mp;
   private JFrame frame;

   private static Font makeFont(TextField tf) {
      return new Font("Helvetica", Font.PLAIN, MyTools.atoi(tf.getText()));
   }

   private static Font makeBoldFont(TextField tf) {
      return new Font("Helvetica", Font.BOLD, MyTools.atoi(tf.getText()));
   }

   private Panel makeFontPanel(String desc, TextField tf) {
      Panel fp = new Panel();
      fp.setLayout(new FlowLayout(FlowLayout.LEFT));
      fp.add(new Label(desc));
      fp.add(tf);
      return fp;
   }

   
   FontPreview(MatrPanel mp_) {
	   frame = new JFrame("Choose Font Sizes");
      mp = mp_;
      frame.setBounds(250,250,320,360);

      frame.setLayout(new BorderLayout());
      Panel p, pFonts, pSamples;

      // content panel
      p = new Panel();
      p.setLayout(new BorderLayout());

      // font sizes
      pFonts = new Panel();
      pFonts.setLayout(new GridLayout(0,1));
      pFonts.add(makeFontPanel("Log Text:", tfrouteLogFont));
      pFonts.add(makeFontPanel("Single Routes:", tfmedRouteFont));
      pFonts.add(makeFontPanel("Paired Routes:", tfsmallRouteFont));
      pFonts.add(makeFontPanel("Town Names:", tftownFont));
      pFonts.add(makeFontPanel("Direction Tab:", tfdirTabFont));
      pFonts.add(makeFontPanel("Route Marker:", tfrouteTabFont));
      pFonts.add(makeFontPanel("Street Names:", tfstreetTabFont));
      p.add("Center", pFonts);

      // samples
      pSamples = new Panel();
      pSamples.setLayout(new BorderLayout());
      pSamples.add("North", tfLogSample);
      pSamples.add("Center", map);
      pSamples.add("South", sign);
      p.add("East", pSamples);

      frame.add("Center", p);

      p = new Panel();
      p.setLayout(new FlowLayout(FlowLayout.RIGHT));
      p.add(bApply);
      p.add(bClose);
      frame.add("South", p);

   }
   
   public void show() {
	   frame.setVisible(true);
   }

   // buttons
//   @Override
//public boolean action(Event e, Object o) {
//      if (e.target == bApply) {
//         Empire.routeLogFont = makeFont(tfrouteLogFont);
//         Empire.townFont = makeFont(tftownFont);
//         Empire.dirTabFont = makeFont(tfdirTabFont);
//         Empire.routeTabFont = makeBoldFont(tfrouteTabFont);
//
//         SignUtils.setFont(makeFont(tfstreetTabFont));
//         MapUtils.setFont(MapUtils.medRoute, makeFont(tfmedRouteFont));
//         MapUtils.setFont(MapUtils.smallRoute, makeFont(tfsmallRouteFont));
//
//         tfLogSample.setFont(Empire.routeLogFont);
//         mp.fontChanged();
//         map.repaint();
//         sign.repaint();
//         validate();
//         return true;
//      }
//      if (e.target == bClose) {
//    	  setVisible(false);
//         return true;
//      }
//     return super.action(e,o);
//   }
}
