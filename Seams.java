import java.util.ArrayDeque;
import java.util.ArrayList;
import tester.*;
import javalib.impworld.*;
import javalib.worldimages.*;
import java.awt.Color;
import java.util.Iterator;
import java.util.Deque;
import java.util.HashSet;

/*
 * In Collaboration with Antoine A. 
 */

//Represents the information of a pixel
class SeamInfo {
  // the pixel
  Pixel p;
  // the total weight including its parents
  double totalWeight;
  // previous SeamInfo
  SeamInfo cameFrom;
  // True-vertical False-horizontal
  boolean direction;

  SeamInfo(Pixel p, SeamInfo cameFrom) {
    this.p = p;
    if (cameFrom == null) {
      totalWeight = p.energy();
    }
    else {
      this.totalWeight = p.energy() + cameFrom.totalWeight;
    }
    this.cameFrom = cameFrom;
    this.direction = true;
  }
}

//Represents a pixel
class Pixel implements Iterable<Pixel> {
  // Color
  Color c;
  // Pixel directions
  Pixel up;
  Pixel down;
  Pixel left;
  Pixel right;

  // The SeamInfo that points to this pixel
  SeamInfo parent;

  Pixel(Color c) {
    this(c, null, null, null, null, null);
  }

  Pixel(Color c, Pixel up, Pixel down, Pixel left, Pixel right, SeamInfo parent) {
    this.c = c;
    this.up = up;
    this.down = down;
    this.left = left;
    this.right = right;
    this.parent = null;
  }

  // Effect changes color
  void updateColor(Color c) {
    this.c = c;
  }

  // returns the brightness of the pixel
  double brightness() {
    return (this.c.getRed() + this.c.getBlue() + this.c.getGreen()) / (3 * 255.0);
  }

  // returns the energy of the pixel according to algorithm
  double energy() {

    // brightnesses of adjacent pixels
    double brA = 0;
    double brB = 0;
    double brC = 0;
    double brD = 0;

    double brF = 0;
    double brG = 0;
    double brH = 0;
    double brI = 0;

    // set the brightnesses with caution of edges

    if (this.up != null && this.up.left != null) {
      brA = this.up.left.brightness();
    }

    if (this.up != null) {
      brB = this.up.brightness();
    }

    if (this.up != null && this.up.right != null) {
      brC = this.up.right.brightness();
    }

    if (this.left != null) {
      brD = this.left.brightness();
    }

    if (this.right != null) {
      brF = this.right.brightness();
    }

    if (this.down != null && this.down.left != null) {
      brG = this.down.left.brightness();
    }

    if (this.down != null) {
      brH = this.down.brightness();
    }

    if (this.down != null && this.down.right != null) {
      brI = this.down.right.brightness();
    }

    // compute the energy

    double horizEnergy = (brA + (2 * brD) + brG) - (brC + (2 * brF) + brI);
    double vertEnergy = (brA + (2 * brB) + brC) - (brG + (2 * brH) + brI);

    double energy = Math.sqrt(Math.pow(horizEnergy, 2) + Math.pow(vertEnergy, 2));

    return energy;
  }

  // returns default iterator left to right
  public Iterator<Pixel> iterator() {
    return new PixelIteratorHorizontal(this);
  }

  // returns up to down iterator
  public Iterator<Pixel> iteratorVertical() {
    return new PixelIteratorVertical(this);
  }
}

//Iterate over pixel from left to right
class PixelIteratorHorizontal implements Iterator<Pixel> {
  Pixel cur;

  PixelIteratorHorizontal(Pixel p) {
    this.cur = p;
  }

  public boolean hasNext() {
    return cur != null;
  }

  public Pixel next() {
    Pixel next = cur;
    cur = next.right;
    return next;
  }
}

//Iterate over pixel from top to bottom
class PixelIteratorVertical implements Iterator<Pixel> {
  Pixel cur;

  PixelIteratorVertical(Pixel p) {
    this.cur = p;
  }

  public boolean hasNext() {
    return cur != null;
  }

  public Pixel next() {
    Pixel next = cur;
    cur = next.down;
    return next;
  }

}

// represents a grid of pixels
class Grid {

  // top left pixel
  Pixel topLeft;

  // the height of the image in pixels
  int height;
  // width of image
  int width;

  // Deque of removedSeams as a stack
  Deque<SeamInfo> removedSeams;

//Constructor purely to aid with testing
  Grid(ArrayList<ArrayList<Pixel>> list) {

    // go thru each y pixel row
    for (int y = 0; y < list.size(); y += 1) {
      // go thru each x pixel in the row
      for (int x = 0; x < list.get(y).size(); x += 1) {
        Pixel p = list.get(y).get(x);
        if (x == 0 && y == 0) {
          this.topLeft = p;
        }
        if (x > 0) {
          p.left = list.get(y).get(x - 1);
        }
        if (x < list.size() - 1) {
          p.right = list.get(y).get(x + 1);
        }
        if (y > 0) {
          p.up = list.get(y - 1).get(x);
        }
        if (y < list.size() - 1) {
          p.down = list.get(y + 1).get(x);
        }
      }
    }

    this.width = list.get(0).size();
    this.height = list.size();
    this.removedSeams = new ArrayDeque<>();

  }

  Grid(String path) {
    ArrayList<ArrayList<Pixel>> grid = new ArrayList<>();
    FromFileImage image = new FromFileImage(path);

    // create the pixel instances

    // go thru each y pixel row
    for (int y = 0; y < image.getHeight(); y += 1) {
      ArrayList<Pixel> row = new ArrayList<>();
      // go thru each x pixel in the row
      for (int x = 0; x < image.getWidth(); x += 1) {
        Color c = image.getColorAt(x, y);
        Pixel p = new Pixel(c);
        if (x == 0 && y == 0) {
          this.topLeft = p;
        }
        row.add(p);
      }
      grid.add(row);
    }

    // set fields based on grid

    this.width = grid.get(0).size();
    this.height = grid.size();
    this.removedSeams = new ArrayDeque<>();

    // connect the pixels

    // go thru each y pixel row
    for (int y = 0; y < image.getHeight(); y += 1) {
      // go thru each x pixel in the row
      for (int x = 0; x < image.getWidth(); x += 1) {
        Pixel p = grid.get(y).get(x);

        if (x > 0) {
          p.left = grid.get(y).get(x - 1);
        }
        if (x < image.getWidth() - 1) {
          p.right = grid.get(y).get(x + 1);
        }
        if (y > 0) {
          p.up = grid.get(y - 1).get(x);
        }
        if (y < image.getHeight() - 1) {
          p.down = grid.get(y + 1).get(x);
        }
      }
    }
  }

  // connects the pixels together given the grid
  // WORKS AS INTENDED BUT DID NOT WORK WITH BIG BANG FOR SOME REASON
  void pointPixels(ArrayList<ArrayList<Pixel>> grid) {
    // go thru each y pixel row
    for (int y = 0; y < grid.size(); y += 1) {
      // go thru each x pixel in the row
      for (int x = 0; x < grid.get(y).size(); x += 1) {
        Pixel p = grid.get(y).get(x);
        if (x == 0 && y == 0) {
          this.topLeft = p;
        }
        if (x > 0) {
          p.left = grid.get(y).get(x - 1);
        }
        if (x < grid.size() - 1) {
          p.right = grid.get(y).get(x + 1);
        }
        if (y > 0) {
          p.up = grid.get(y - 1).get(x);
        }
        if (y < grid.size() - 1) {
          p.down = grid.get(y + 1).get(x);
        }
      }
    }
  }

  // Returns an image of the rendered pixels
  // display depends on colorMode input, where
  // 0 represents displaying the image in it's original color
  // 1 represents displaying the image energy in grayscale
  // 2 represents displaying the total weights
  WorldImage render(int colorMode, SeamInfo min) {

    // creates a image of desired dimensions with empty-colored cells
    ComputedPixelImage image = new ComputedPixelImage(this.width, this.height);

    // check given SeamInfo is not null. If it isn't there is something to paint red
    HashSet<Pixel> redPixels = new HashSet<>();

    if (min != null) {
      // Iterate through Seams until there is no more and add their pixels to the set
      while (min != null) {
        redPixels.add(min.p);
        min = min.cameFrom;
      }
    }

    // top-left-most pixel
    Pixel p = this.topLeft;

    // keep track of coordinate
    int x = 0;
    int y = 0;

    // iterates down the image
    while (p != null) {
      Pixel pRow = p;

      // iterates horizontally per row
      while (pRow != null) {
        Color color = pRow.c;

        if (colorMode == 1) {
          // convert a pixel's energy to its brightness
          int eConversion = (int) (pRow.energy() * (255.0 / Math.sqrt(32)));

          // just in case doubles act funky
          if (eConversion > 255) {
            eConversion = 255;
          }

          color = new Color(eConversion, eConversion, eConversion);

        }
        else if (colorMode == 2) {
          // convert a pixel's energy to its SeamInfo's cumulative energy
          int eConversion = (int) (pRow.parent.totalWeight * (5));

          // just in case doubles act funky
          if (eConversion > 255) {
            eConversion = 255;
          }

          color = new Color(eConversion, eConversion, eConversion);

        }

        if (redPixels.contains(pRow)) {
          color = new Color(255, 0, 0);
        }

        image.setPixel(x, y, color);

        pRow = pRow.right;
        x += 1;
      }

      p = p.down;

      x = 0;
      y += 1;
    }

    return image;
  }

  // return the most boring seam
  SeamInfo minimumVertical() {
    ArrayList<SeamInfo> pixelPaths = new ArrayList<>();
    // iterate through the toprow until there's nothing left
    for (Pixel p : this.topLeft) {
      SeamInfo target = new SeamInfo(p, null);
      p.parent = target;
      pixelPaths.add(target);
    }

    Pixel tracker = this.topLeft.down;

    // iterate through the rows until we run out of rows
    while (tracker != null) {
      ArrayList<SeamInfo> newPixelPaths = new ArrayList<>();

      int col = 0;

      for (Pixel p : tracker) {
        // We know that there is always a row above us since this loop runs after adding
        // the first row to the path
        SeamInfo left = null;
        SeamInfo up = pixelPaths.get(col);
        SeamInfo right = null;
        Double leftWeight = Double.MAX_VALUE;
        Double middleWeight = up.totalWeight;
        Double rightWeight = Double.MAX_VALUE;

        // check we're not on left edge
        if (col > 0) {
          left = pixelPaths.get(col - 1);
          leftWeight = left.totalWeight;
        }

        // check we're not on right
        if (col < this.width - 1) {
          right = pixelPaths.get(col + 1);
          rightWeight = right.totalWeight;
        }

        if (leftWeight <= middleWeight && leftWeight <= rightWeight) {
          // left side lightest
          SeamInfo newSeam = new SeamInfo(p, left);
          newSeam.direction = true;
          p.parent = newSeam;
          newPixelPaths.add(newSeam);
        }
        else if (middleWeight <= leftWeight && middleWeight <= rightWeight) {
          // middle lightest
          SeamInfo newSeam = new SeamInfo(p, up);
          newSeam.direction = true;
          p.parent = newSeam;
          newPixelPaths.add(newSeam);
        }
        else {
          // right lightest
          SeamInfo newSeam = new SeamInfo(p, right);
          newSeam.direction = true;
          p.parent = newSeam;
          newPixelPaths.add(newSeam);
        }
        col += 1;
      }

      // update
      pixelPaths = newPixelPaths;
      tracker = tracker.down;
    }

    // find cheapest seam out of paths
    SeamInfo min = pixelPaths.get(0);
    for (SeamInfo seam : pixelPaths) {
      if (seam.totalWeight < min.totalWeight) {
        min = seam;
      }
    }

    return min;
  }

  // effect: Removes the vertical seam with the lowest energy
  void removeSeamVertical(SeamInfo min) {

    // to prevent removing a seam from trivial data
    // (and to prevent crashing)
    if (this.width <= 1) {
      return;
    }

    // SeamInfo min = this.minimumVertical();

    // decrease width by 1
    this.width -= 1;

    // iterate through the entire seam chain until we run out
    while (min.cameFrom != null) {
      Pixel currentPixel = min.p;
      Pixel nextPixel = min.cameFrom.p;

      // swap horizontally
      if (currentPixel.left != null && currentPixel.right != null) {
        currentPixel.left.right = currentPixel.right;
        currentPixel.right.left = currentPixel.left;
      }
      else if (currentPixel.left == null) {
        currentPixel.right.left = null;
      }
      else if (currentPixel.right == null) {
        currentPixel.left.right = null;
      }

      if (currentPixel.up != null) {
        if (currentPixel.up.right == nextPixel) {
          // cutting to the top right
          currentPixel.right.up = currentPixel.up;
          currentPixel.up.down = currentPixel.right;
        }
        else if (currentPixel.up.left == nextPixel) {
          // cutting to top left
          currentPixel.left.up = currentPixel.up;
          currentPixel.up.down = currentPixel.left;
        }
      }

      // Don't throw away pointers to other pixels so it knows where to re-insert

      this.removedSeams.addFirst(min);
      min = min.cameFrom;
    }

    Pixel lastPixel = min.p;

    // top row
    // swap horizontally
    if (lastPixel.left != null && lastPixel.right != null) {
      lastPixel.left.right = lastPixel.right;
      lastPixel.right.left = lastPixel.left;
    }
    else if (lastPixel.left == null) {
      lastPixel.right.left = null;
    }
    else if (lastPixel.right == null) {
      lastPixel.left.right = null;
    }

    if (this.topLeft == lastPixel) {
      this.topLeft = lastPixel.right;
    }
  }

  // return the most boring seam horizontally
  SeamInfo minimumHorizontal() {
    ArrayList<SeamInfo> pixelPaths = new ArrayList<>();

    Iterator<Pixel> verticalIterator = this.topLeft.iteratorVertical();
    // iterate through the left column until there's nothing left
    while (verticalIterator.hasNext()) {
      pixelPaths.add(new SeamInfo(verticalIterator.next(), null));
    }

    // Keep track of which column we are on
    Pixel tracker = this.topLeft.right;

    // iterate through the column until we run out of columns
    while (tracker != null) {
      ArrayList<SeamInfo> newPixelPaths = new ArrayList<>();

      int row = 0;

      Iterator<Pixel> columnIterate = tracker.iteratorVertical();

      while (columnIterate.hasNext()) {
        // We know that there is always a column left to us since this loop runs after
        // adding the first row to the path
        Pixel next = columnIterate.next();
        SeamInfo top = null;
        SeamInfo left = pixelPaths.get(row);
        SeamInfo bottom = null;
        Double topWeight = Double.MAX_VALUE;
        Double middleWeight = left.totalWeight;
        Double bottomWeight = Double.MAX_VALUE;

        // check we're not on ceiling
        if (row > 0) {
          bottom = pixelPaths.get(row - 1);
          bottomWeight = bottom.totalWeight;
        }

        // check we're not on floor
        if (row < this.height - 1) {
          top = pixelPaths.get(row + 1);
          topWeight = top.totalWeight;
        }

        if (bottomWeight <= middleWeight && middleWeight <= topWeight) {
          // bottom left side lightest
          SeamInfo newSeam = new SeamInfo(next, bottom);
          next.parent = newSeam;
          newPixelPaths.add(newSeam);
        }
        else if (middleWeight <= bottomWeight && middleWeight <= topWeight) {
          // middle lightest
          SeamInfo newSeam = new SeamInfo(next, left);
          next.parent = newSeam;
          newPixelPaths.add(newSeam);
        }
        else {
          // top left lightest
          SeamInfo newSeam = new SeamInfo(next, top);
          next.parent = newSeam;
          newPixelPaths.add(newSeam);
        }
        row += 1;
      }

      // update
      pixelPaths = newPixelPaths;
      tracker = tracker.right;
    }

    // find cheapest seam out of paths
    SeamInfo min = pixelPaths.get(0);
    for (SeamInfo seam : pixelPaths) {
      if (seam.totalWeight < min.totalWeight) {
        min = seam;
      }
    }

    return min;
  }

  // removes the cheapest horizontal seam
  void removeSeamHorizontal(SeamInfo min) {

    // to prevent removing a seam from trivial data
    // (and to prevent crashing)
    if (this.height <= 1) {
      return;
    }

    // decrease height by 1
    this.height -= 1;

    // iterate through the entire seam chain until we run out
    while (min.cameFrom != null) {
      Pixel currentPixel = min.p;
      Pixel nextPixel = min.cameFrom.p;

      // swap vertically
      if (currentPixel.up != null && currentPixel.down != null) {
        currentPixel.up.down = currentPixel.down;
        currentPixel.down.up = currentPixel.up;
      }
      else if (currentPixel.down == null) {
        currentPixel.up.down = null;
      }
      else if (currentPixel.up == null) {
        currentPixel.down.up = null;
      }

      // ensure we don't get a null pointer
      if (currentPixel.left != null) {
        // check which direction we're cutting in and swap accordingly
        if (currentPixel.left.up == nextPixel) {
          // cutting above left
          currentPixel.left.right = currentPixel.up;
          currentPixel.up.left = currentPixel.left;
        }
        else if (currentPixel.left.down == nextPixel) {
          // cutting below left
          currentPixel.left.right = currentPixel.down;
          currentPixel.down.left = currentPixel.left;
        }
      }

      // We don't clear the pointers so that each pixel knows where to re-insert
      // themselves.
      this.removedSeams.addFirst(min);
      min = min.cameFrom;
    }

    Pixel lastPixel = min.p;

    // left column
    // swap vertically
    if (lastPixel.up != null && lastPixel.down != null) {
      lastPixel.up.down = lastPixel.down;
      lastPixel.down.up = lastPixel.up;
    }
    else if (lastPixel.down == null) {
      lastPixel.up.down = null;
    }
    else if (lastPixel.up == null) {
      lastPixel.down.up = null;
    }

    if (this.topLeft == lastPixel) {
      this.topLeft = lastPixel.down;
    }
  }

  // EXTRA CREDIT METHODS FROM HERE AND BELOW:

  // Paints every pixel in the seam red
  void paintRed(SeamInfo seam) {
    if (seam == null) {
      return;
    }
    else {
      seam.p.updateColor(new Color(255, 0, 0));
      this.paintRed(seam.cameFrom);
    }
  }
}

// represents a compress world (for big bang)
class CompressWorld extends World {

  // the grid of the image to compress
  Grid grid;
  SeamInfo min;

  boolean pause;
  boolean cutVertically;
  // boolean reverse;

  // where 0 represents displaying the original colored image,
  // where 1 represents displaying the image in gray scale
  // where 2 represents displaying the total weights
  int colorMode;

  // where 0 represents no task is assigned,
  // 1 represents to remove this.min vertically
  // 2 represents to remove this.min horizontally
  int nextTask;

  CompressWorld(Grid grid) {
    this.grid = grid;
    this.min = null;
    this.cutVertically = true;
    this.pause = false;
    this.nextTask = 0;
    this.colorMode = 0;
    // this.reverse = false;
  }

  // draw out the image scene
  public WorldScene makeScene() {
    WorldScene canvas = new WorldScene(1200, 1000);
    canvas.placeImageXY(grid.render(this.colorMode, this.min), 600, 500);
    return canvas;
  }

  // user input based on assignment details (extra credit)
  public void onKeyEvent(String key) {
    if (key.equals("v")) {
      this.cutVertically = true;
    }
    else if (key.equals("h")) {
      this.cutVertically = false;
    }
    else if (key.equals(" ")) {
      this.pause = !this.pause;
    }
    else if (key.equals("c")) {
      this.colorMode = (this.colorMode + 1) % 3;
    }
  }

  // ticks the world by one frame
  public void onTick() {
    if (!this.pause) {

      if (this.min != null && this.nextTask > 0) {
        if (this.nextTask == 1) {
          this.grid.removeSeamVertical(this.min);
        }
        else {
          this.grid.removeSeamHorizontal(this.min);
        }
        this.nextTask = 0;
        this.min = null;
      }
      else {
        if (this.cutVertically) {
          this.min = this.grid.minimumVertical();
          this.nextTask = 1;
        }
        else {
          this.min = this.grid.minimumHorizontal();
          this.nextTask = 2;
        }
        this.grid.paintRed(this.min);
      }
    }
  }

}

// examples class
class ExamplesImages {
  Pixel A1;
  Pixel A2;
  Pixel A3;
  Pixel A4;
  Pixel B1;
  Pixel B2;
  Pixel B3;
  Pixel B4;
  Pixel C1;
  Pixel C2;
  Pixel C3;
  Pixel C4;
  Pixel D1;
  Pixel D2;
  Pixel D3;
  Pixel D4;

  Grid grid;
  Grid grid2;
  Grid grid3;

  // initializes data
  void init() {
    A1 = new Pixel(new Color(68, 128, 188));
    A2 = new Pixel(new Color(64, 124, 184));
    A3 = new Pixel(new Color(182, 122, 62));
    A4 = new Pixel(new Color(76, 116, 156));
    B1 = new Pixel(new Color(90, 140, 190));
    B2 = new Pixel(new Color(100, 165, 230));
    B3 = new Pixel(new Color(79, 119, 159));
    B4 = new Pixel(new Color(0, 0, 0));
    C1 = new Pixel(new Color(0, 0, 0));
    C2 = new Pixel(new Color(255, 255, 255));
    C3 = new Pixel(new Color(10, 16, 22));
    C4 = new Pixel(new Color(0, 22, 44));
    D1 = new Pixel(new Color(30, 15, 0));
    D2 = new Pixel(new Color(36, 18, 0));
    D3 = new Pixel(new Color(24, 48, 0));
    D4 = new Pixel(new Color(0, 54, 27));

    ArrayList<Pixel> row1 = new ArrayList<>();
    ArrayList<Pixel> row2 = new ArrayList<>();
    ArrayList<Pixel> row3 = new ArrayList<>();
    ArrayList<Pixel> row4 = new ArrayList<>();
    ArrayList<ArrayList<Pixel>> finalGrid = new ArrayList<>();

    row1.add(A1);
    row1.add(A2);
    row1.add(A3);
    row1.add(A4);
    row2.add(B1);
    row2.add(B2);
    row2.add(B3);
    row2.add(B4);
    row3.add(C1);
    row3.add(C2);
    row3.add(C3);
    row3.add(C4);
    row4.add(D1);
    row4.add(D2);
    row4.add(D3);
    row4.add(D4);

    finalGrid.add(row1);
    finalGrid.add(row2);
    finalGrid.add(row3);
    finalGrid.add(row4);

    grid = new Grid(finalGrid);
    // Change the string to another path for different images
    grid2 = new Grid("/Users/jacobkim/EclipseFiles/EclipseWorkspace/Assignment9/src/balloon.png");

  }

  void testPointPixels(Tester t) {
    this.init();

    // this method is called in any constructor, ensuring that pixels
    // are pointed correctly, so init already calls it.

    t.checkExpect(D3.up.left.down.right, D3);
    t.checkExpect(D4.up, C4);
    t.checkExpect(A1.up, null);
    t.checkExpect(B4.right, null);
    t.checkExpect(C2.left.right, C2);
  }

  void testBrightness(Tester t) {
    this.init();
    t.checkInexact(A1.brightness(), 128 / 255.0, .01);
    t.checkInexact(A2.brightness(), 124 / 255.0, .01);
    t.checkInexact(A3.brightness(), 122 / 255.0, .01);
    t.checkInexact(A4.brightness(), 116 / 255.0, .01);

    t.checkInexact(B1.brightness(), 140 / 255.0, .01);
    t.checkInexact(B2.brightness(), 165 / 255.0, .01);
    t.checkInexact(B3.brightness(), 119 / 255.0, .01);
    t.checkInexact(B4.brightness(), 0 / 255.0, .01);

    t.checkInexact(C1.brightness(), 0 / 255.0, .01);
    t.checkInexact(C2.brightness(), 255 / 255.0, .01);
    t.checkInexact(C3.brightness(), 16 / 255.0, .01);
    t.checkInexact(C4.brightness(), 22 / 255.0, .01);

    t.checkInexact(D1.brightness(), 15 / 255.0, .01);
    t.checkInexact(D2.brightness(), 18 / 255.0, .01);
    t.checkInexact(D3.brightness(), 24 / 255.0, .01);
    t.checkInexact(D4.brightness(), 27 / 255.0, .01);

  }

  void testEnergy(Tester t) {
    this.init();
    t.checkInexact(A1.energy(), 2.38, .01);
    t.checkInexact(A2.energy(), 2.31, .01);
    t.checkInexact(A3.energy(), 1.73, .01);
    t.checkInexact(A4.energy(), 1.50, .01);

    t.checkInexact(B1.energy(), 2.82, .01);
    t.checkInexact(B2.energy(), .17, .02);
    t.checkInexact(B3.energy(), 2.34, .01);
    t.checkInexact(B4.energy(), 1.87, .01);

    t.checkInexact(C1.energy(), 3.13, .01);
    t.checkInexact(C2.energy(), 2.02, .01);
    t.checkInexact(C3.energy(), 2.73, .01);
    t.checkInexact(C4.energy(), .70, .01);

    t.checkInexact(D1.energy(), 1.52, .01);
    t.checkInexact(D2.energy(), 2.07, .01);
    t.checkInexact(D3.energy(), 1.48, .01);
    t.checkInexact(D4.energy(), .34, .02);
  }

  void testPaintRed(Tester t) {
    this.init();
    SeamInfo seam = grid.minimumVertical();
    grid.paintRed(seam);

    t.checkExpect(A1.c, new Color(68, 128, 188));
    t.checkExpect(A2.c, new Color(64, 124, 184));
    t.checkExpect(A3.c, new Color(182, 122, 62));
    t.checkExpect(A4.c, new Color(255, 0, 0)); // RED
    t.checkExpect(B1.c, new Color(90, 140, 190));
    t.checkExpect(B2.c, new Color(100, 165, 230));
    t.checkExpect(B3.c, new Color(79, 119, 159));
    t.checkExpect(B4.c, new Color(255, 0, 0)); // RED
    t.checkExpect(C1.c, new Color(0, 0, 0));
    t.checkExpect(C2.c, new Color(255, 255, 255));
    t.checkExpect(C3.c, new Color(10, 16, 22));
    t.checkExpect(C4.c, new Color(255, 0, 0)); // RED
    t.checkExpect(D1.c, new Color(30, 15, 0));
    t.checkExpect(D2.c, new Color(36, 18, 0));
    t.checkExpect(D3.c, new Color(24, 48, 0));
    t.checkExpect(D4.c, new Color(255, 0, 0)); // RED
  }

  // FIND MOST BORING SEAM TESTS
  void testMinimumVertical(Tester t) {
    this.init();
    SeamInfo seam = grid.minimumVertical();
    SeamInfo ans = new SeamInfo(D4, new SeamInfo(C4, new SeamInfo(B4, new SeamInfo(A4, null))));
    t.checkExpect(seam, ans);

    grid.removeSeamVertical(seam);
    SeamInfo seam2 = grid.minimumVertical();
    SeamInfo ans2 = new SeamInfo(D1, new SeamInfo(C2, new SeamInfo(B2, new SeamInfo(A3, null))));
    t.checkExpect(seam2, ans2);
  }

  void testMinHorizontal(Tester t) {
    this.init();
    SeamInfo min1 = this.grid.minimumHorizontal();
    SeamInfo ans = new SeamInfo(D4, new SeamInfo(D3, new SeamInfo(C2, new SeamInfo(D1, null))));
    t.checkExpect(min1, ans);

    this.grid.removeSeamHorizontal(min1);

    SeamInfo min2 = this.grid.minimumHorizontal();
    SeamInfo ans2 = new SeamInfo(C4, new SeamInfo(C3, new SeamInfo(B2, new SeamInfo(C1, null))));
    t.checkExpect(min2, ans2);
  }

  // REMOVE TESTS

  void testRemoveVertical(Tester t) {
    this.init();
    ComputedPixelImage ansGrid = new ComputedPixelImage(3, 4);
    ansGrid.setPixel(0, 0, A1.c);
    ansGrid.setPixel(1, 0, A2.c);
    ansGrid.setPixel(2, 0, A3.c);
    ansGrid.setPixel(0, 1, B1.c);
    ansGrid.setPixel(1, 1, B2.c);
    ansGrid.setPixel(2, 1, B3.c);
    ansGrid.setPixel(0, 2, C1.c);
    ansGrid.setPixel(1, 2, C2.c);
    ansGrid.setPixel(2, 2, C3.c);
    ansGrid.setPixel(0, 3, D1.c);
    ansGrid.setPixel(1, 3, D2.c);
    ansGrid.setPixel(2, 3, D3.c);

    grid.removeSeamVertical(this.grid.minimumVertical());
    t.checkExpect(grid.render(0, null), ansGrid);

    grid.removeSeamVertical(this.grid.minimumVertical());

    ComputedPixelImage ansGrid2 = new ComputedPixelImage(2, 4);
    ansGrid2.setPixel(0, 0, A1.c);
    ansGrid2.setPixel(1, 0, A2.c);
    ansGrid2.setPixel(0, 1, B1.c);
    ansGrid2.setPixel(1, 1, B3.c);
    ansGrid2.setPixel(0, 2, C1.c);
    ansGrid2.setPixel(1, 2, C3.c);
    ansGrid2.setPixel(0, 3, D2.c);
    ansGrid2.setPixel(1, 3, D3.c);

    t.checkExpect(grid.render(0, null), ansGrid2);

  }

  void testRemoveHorizontal(Tester t) {
    this.init();
    ComputedPixelImage ansGrid = new ComputedPixelImage(4, 3);
    ansGrid.setPixel(0, 0, A1.c);
    ansGrid.setPixel(1, 0, A2.c);
    ansGrid.setPixel(2, 0, A3.c);
    ansGrid.setPixel(3, 0, A4.c);
    ansGrid.setPixel(0, 1, B1.c);
    ansGrid.setPixel(1, 1, B2.c);
    ansGrid.setPixel(2, 1, B3.c);
    ansGrid.setPixel(3, 1, B4.c);
    ansGrid.setPixel(0, 2, C1.c);
    ansGrid.setPixel(1, 2, D2.c);
    ansGrid.setPixel(2, 2, C3.c);
    ansGrid.setPixel(3, 2, C4.c);

    grid.removeSeamHorizontal(this.grid.minimumHorizontal());
    t.checkExpect(grid.render(0, null), ansGrid);

    grid.removeSeamHorizontal(this.grid.minimumHorizontal());

    ComputedPixelImage ansGrid2 = new ComputedPixelImage(4, 2);
    ansGrid2.setPixel(0, 0, A1.c);
    ansGrid2.setPixel(1, 0, A2.c);
    ansGrid2.setPixel(2, 0, A3.c);
    ansGrid2.setPixel(3, 0, A4.c);
    ansGrid2.setPixel(0, 1, B1.c);
    ansGrid2.setPixel(1, 1, D2.c);
    ansGrid2.setPixel(2, 1, B3.c);
    ansGrid2.setPixel(3, 1, B4.c);

    t.checkExpect(grid.render(0, null), ansGrid2);

  }

  // big bang
  void testBigBang(Tester t) {
    this.init();
    double tickRate = .01;

    World compress = new CompressWorld(grid2);
    compress.bigBang(1200, 1000, tickRate);
  }
}
