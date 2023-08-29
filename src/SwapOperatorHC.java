import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.LinkedList;
import java.util.Random;

public class SwapOperatorHC {
    private boolean visualize = false;
    private Random r = new Random(1);
    private String file;

    private int MAX_DEPTH = 4;

    /**Global Graph Variables**/
    private Graph g;
    private BitMatrix neighbours;
    private int[] D;
    private int[] tempD;

    /**Global Coloring Variables**/
    private int NR_OF_COLORS;
    private int best_NR_OF_COLORS;
    private BitMatrix currentColorSets;
    private BitMatrix newColorSets;
    private BitMatrix bestColoring;
    private int sumPresentConflicts;

    /**Local Search Global Variables**/
    private long swapOperations;
    private long swapImprovements;
    private long newColorAssignments;
    private long swapChains;
    private boolean swapped;
    private int colorReductions;
    private int iterations;

    //global parameters
    private int ConflictSumReduction;
    private int timesAcceptedSolutions;

    private final boolean GRAPHICS = false;
    private final boolean GRAPHICSD = false;
    private ArrayList<Long> times = new ArrayList<>();
    private ArrayList<Long> values = new ArrayList<>();
    private ArrayList<ArrayList<Long>> dValues = new ArrayList<>();

    private ArrayList<ArrayList<Integer>> nodeColors = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        new SwapOperatorHC("src/Files/seconds/david.txt");
        //new SwapOperatorHC1(args[0]);
    }

    public SwapOperatorHC(String f) throws IOException {
        file = f;
        colorGraph();
    }

    public void colorGraph() throws IOException {
        /**experiment Parameters**/
        swapOperations = 0;
        swapImprovements = 0;
        newColorAssignments = 0;
        colorReductions = 0;
        iterations = 0;

        //global parameters
        long duration = 0;
        sumPresentConflicts = 0;
        timesAcceptedSolutions = 0;
        int Sum_Best_NR_OF_COLORS = 0;
        ConflictSumReduction = 0;

        /**experiment Parameters**/

        g = new Graph(file);

        neighbours = new BitMatrix(g.getNrVertices(), g.getNrVertices());
        for (int i = 0; i < g.getNrVertices(); i++) {
            LinkedList<Integer> adjList = g.getNeighbors(i);
            BitSet temp = new BitSet(g.getNrVertices());
            for (Integer integer : adjList) {
                if (integer != i) temp.set(integer);
            }
            neighbours.setRow(temp, i);
        }

        NR_OF_COLORS = g.getGraphDegree() + 1;

        long startTime = System.nanoTime();

        currentColorSets = new BitMatrix(NR_OF_COLORS, g.getNrVertices());

        initialColoring();

        int intialColors = NR_OF_COLORS;

        best_NR_OF_COLORS = NR_OF_COLORS;
        //System.out.println(NR_OF_COLORS);

        D = new int[g.getNrVertices()];

        //1 kleurklasse verwijderen & ontkleurde knopen herverdelen over de resterende verzamelingen
        removeColor();

        newColorSets = new BitMatrix(currentColorSets);
        updateD();
        for (int j : D) {
            sumPresentConflicts += j;
        }

        localSearch(startTime);

        //System.out.println("de graaf werd feasible gekleurd in "+best_NR_OF_COLORS+" kleuren");

        long endTime = System.nanoTime();
        duration += (endTime-startTime);
        Sum_Best_NR_OF_COLORS += best_NR_OF_COLORS;
        //System.out.println(file);
        String[] filePath = file.split("/");
        String fileName = filePath[filePath.length-1];
        //System.out.println(fileName);

        System.out.println(duration);

        StringBuilder data = new StringBuilder(fileName+ "," + Sum_Best_NR_OF_COLORS + "," + duration+ "," + swapOperations +
                "," + swapImprovements + "," + newColorAssignments + "," + timesAcceptedSolutions + ","+ConflictSumReduction + "," + swapOperations/swapChains+","+ intialColors+","+colorReductions+","+iterations);

        /*if (GRAPHICS) {
            System.out.println("sumPresentConflicts");
            for (int i = 0; i < values.size(); i++) {
                System.out.print(values.get(i)+","+times.get(i));
            }
            System.out.println();
        }

        if (GRAPHICSD) {
            System.out.println("D");
            for (int i = 0; i < g.getNrVertices(); i++) {
                for (int j = 0; j < dValues.get(i).size(); j++) {
                    System.out.print(dValues.get(i).get(j)+","+times.get(j));
                }
            }
            System.out.println();
        }*/

        //printNodeColors(currentColorSets);
        validator();

        System.out.println("Tijden in ns:");
        System.out.println("avg duration: "+ duration);

        System.out.println("G,HC X(G),total duration,#swap,#swap improvement,#new color assignment,# worse solutions accepted,#sum conflicts reduction, avg depth of swap chain, Init X(G), #colorreductions, iteraties");
        System.out.println(data);
    }

    public void printNodeColors(BitMatrix b) {
        System.out.print("colors: ");
        for (int i = 0; i < g.getNrVertices(); i++) {
            System.out.print(getColor(i, b)+" ");
        }
        System.out.println();
    }

    public void printD(long time) {
        System.out.print("D: "+time+" ");
        for (int i = 0; i < g.getNrVertices(); i++) {
            System.out.print(D[i]+" ");
        }
        System.out.println();
    }

    public void printSumPresentConflicts(long time) {
        System.out.println("CH: "+sumPresentConflicts+" "+time);
    }

    public void printColorRemoved(long time) {
        System.out.println("CR: "+time);
    }

    /**LocalSearch**/
    public void localSearch(long absStartTime) {
        for (int i = 0; i < g.getNrVertices(); i++) {
            dValues.add(new ArrayList<>());
        }
        double T = 10000;
        double T_MIN = 0.001;
        int iteraties;
        int iteratiesWI;
        int eqIterations1 = 2*g.getNrEdges()/(g.getNrVertices()-1);
        long timeStart = System.nanoTime();

        LinkedList<int[]> currentConflictedNodes = findConflictingNodes(currentColorSets);
        LinkedList<int[]> newConflictedNodes = findConflictingNodes(newColorSets);

        iteraties=0;

        while (T>T_MIN) {
            iterations+=iteraties;
            System.out.println("T: "+T);
            iteraties=0;
            iteratiesWI = 0;

            while (iteraties<eqIterations1 && iteratiesWI < 10) {
                sumPresentConflicts = 0;
                for (int[] newConflictedNode : currentConflictedNodes) {
                    sumPresentConflicts += D[newConflictedNode[0]];
                }

                if(GRAPHICS || GRAPHICSD) {
                    long currTime = System.nanoTime() - timeStart;
                    times.add(currTime);
                    if (GRAPHICS) {
                        values.add((long) sumPresentConflicts);
                        printSumPresentConflicts(currTime);
                    }

                    if (GRAPHICSD) {
                        for (int i = 0; i < g.getNrVertices(); i++) {
                            dValues.get(i).add((long) D[i]);
                        }
                        printD(currTime);
                    }
                }

                if (currentConflictedNodes.size() > 0) {
                    //perform an operation on NewColorSets
                    int randomConflictedNode = r.nextInt(currentConflictedNodes.size());
                    tempD = new int[D.length];

                    swapped = false;
                    operator(currentConflictedNodes.get(randomConflictedNode)[0], currentConflictedNodes.get(randomConflictedNode)[1], 0);

                    newConflictedNodes.clear();

                    newConflictedNodes = findConflictingNodes(newColorSets);
                }

                int newSumConflicts = 0;
                for (int[] newConflictedNode : newConflictedNodes) {
                    newSumConflicts += D[newConflictedNode[0]] + tempD[newConflictedNode[0]];
                }

                if(newSumConflicts<sumPresentConflicts) {
                    ConflictSumReduction++;
                    if (swapped) swapImprovements++;
                    iteratiesWI = 0;
                    sumPresentConflicts = newSumConflicts;
                    currentColorSets = new BitMatrix(newColorSets);
                    for (int i = 0; i < D.length; i++) {
                        D[i] += tempD[i];
                    }
                    currentConflictedNodes = findConflictingNodes(currentColorSets);
                }
                else if (Math.exp((sumPresentConflicts-newSumConflicts)/T)> r.nextDouble()) {
                    timesAcceptedSolutions++;
                    currentColorSets = new BitMatrix(newColorSets);
                    for (int i = 0; i < D.length; i++) {
                        D[i] += tempD[i];
                    }
                    sumPresentConflicts = newSumConflicts;
                    iteratiesWI = 0;

                    currentColorSets = new BitMatrix(newColorSets);
                    currentConflictedNodes = findConflictingNodes(currentColorSets);
                }
                else {
                    newColorSets = new BitMatrix(currentColorSets);
                    iteratiesWI++;
                }

                if (newSumConflicts == 0) {
                    colorReductions++;
                    if (GRAPHICS || GRAPHICSD) printColorRemoved(System.nanoTime() - timeStart);
                    iteratiesWI = 0;
                    best_NR_OF_COLORS = NR_OF_COLORS;

                    if(GRAPHICS) {
                        long currTime = System.nanoTime() - timeStart;
                        times.add(currTime);
                        values.add((long) sumPresentConflicts);
                    }

                    //kleur verwijderen waarvoor som van conflict counters (D) het laagst is
                    removeColor();
                    //na verwijderne van kleur bevat currentColorSets opnieuw conflicten => lijst aanpassen
                    currentConflictedNodes = findConflictingNodes(currentColorSets);
                }
                updateD();
                iteraties++;
            }
            T*=0.999;
        }
    }

    public void addNodeColorsToList() {
        long currTime = System.nanoTime();
        for (int i = 0; i < g.getNrVertices(); i++) {
            nodeColors.get(i).add(getColor(i, newColorSets));
        }
    }

    public static void printColorSets(BitMatrix b) {
        for (int i = 0; i < b.getRows(); i++) {
            System.out.println(b.getRow(i));
        }
        System.out.println("#########################");
    }

    public void initialColoring() {

        //genereer initiële kleuring mbv greedy algoritme

        //1e knoop (0) toewijzen aan 1e kleurklasse (0)
        currentColorSets.getRow(0).set(0, true);

        for (int v=1; v< g.getNrVertices(); v++) {
            int c = 0;

            currentColorSets.getRow(c).set(v, true);
            //assign(v, c, currentColoring);

            while (neighborConflict(v, c, currentColorSets)){
                currentColorSets.getRow(c).set(v, false);
                c++;
                currentColorSets.getRow(c).set(v, true);
                if(c > NR_OF_COLORS) NR_OF_COLORS = c;
            }
        }
        removeInitialRedundantSets(currentColorSets);
    }

    public boolean neighborConflict(int v, int c, BitMatrix b) {
        //check of een de knopen in de kleurklasse van v buren zijn van v
        BitSet temp = (BitSet) neighbours.getRow(v).clone();
        temp.and(b.getRow(c));
        return temp.cardinality() > 0 && !(temp.cardinality() == 1 && temp.get(v));
    }

    public void removeInitialRedundantSets(BitMatrix b) {
        ArrayList<Integer> rowsToRemove = new ArrayList<>();
        for (int i = b.getRows()-1; i >= 0; i--) {
            if (b.getRow(i).cardinality()==0) rowsToRemove.add(i);
        }
        for (Integer integer : rowsToRemove) {
            b.removeRow(integer);
            NR_OF_COLORS--;
        }
    }

    public void removeColor() {
        bestColoring = new BitMatrix(currentColorSets);
        //vind kleur klasse waarvoor minste conflict counters
        int c = leastConflictCounters();
        currentColorSets.removeRow(c);
        NR_OF_COLORS--;
        recolorNodes();
    }


    public int leastConflictCounters() {
        //kleur klasse vinden waar voor de som van de geschiedenis van conflicten van van knopen in die klasse de kleinste is
        int row=-1;
        int minSum = Integer.MAX_VALUE;
        for (int i = 0; i < currentColorSets.getRows(); i++) {
            BitSet temp = currentColorSets.getRow(i);
            int sum = 0;
            for (int j = 0; j < temp.cardinality(); j++) {
                int v = temp.nextSetBit(j);
                sum += D[v];
            }
            //System.out.println("color: "+i+", Dsum: "+sum+", |V|: "+currentColorSets.getRow(i).cardinality());
            if(sum<minSum) {
                row = i;
                minSum = sum;
            }
        }
        //System.out.println("gekozen kleur set: "+row+", Dsum: "+minSum);
        return row;
    }

    public void recolorNodes() {
        //ongekleurde knopen opnieuw verdelen over overige kleurklassen zodat aantal conflicten (voor die knopen) minimaal is
        int[] uncoloredNodes = findUncoloredNodes();
        for (int i = 0; i < uncoloredNodes.length; i++) {
            int[] colors = new int[NR_OF_COLORS];
            //per kleur set bepalen hoeveel buren erin zitten
            for (int j = 0; j < NR_OF_COLORS; j++) {
                BitSet temp = (BitSet) neighbours.getRow(i).clone();
                temp.and(currentColorSets.getRow(j));
                colors[j] = temp.cardinality();
            }

            int new_color = 0;
            //ongekleurde knoop toewijzen aan kleurset die minste aantal buren bevat => minst nieuwe conflicten
            //indien meedere kleuren zelfde aantal buren => laagste kleur
            int min_neigbours = Integer.MAX_VALUE;
            for (int j = colors.length-1; j >= 0; j--) {
                if (colors[j] <= min_neigbours) {
                    new_color = j;
                    min_neigbours = colors[j];
                }
            }

            currentColorSets.getRow(new_color).set(uncoloredNodes[i]);
        }
        newColorSets = new BitMatrix(currentColorSets);
    }

    public int[] findUncoloredNodes(){
        BitSet temp = (BitSet) currentColorSets.getRow(0).clone();
        for (int i = 1; i < currentColorSets.getRows(); i++) {
            temp.or(currentColorSets.getRow(i));
        }
        //na or met elke rij in currentcolorsets zal de bit van elke knoop die een kleur heeft 1 zijn
        //alle bits na de or flippen
        // enkel bits van knopen die in geen enkele kleurklasse zitten zal 1 zijn
        temp.flip(0, g.getNrVertices());

        int[] uncoloredNodes = new int[temp.cardinality()];
        int nextSetBit = 0;
        for (int i = 0; i < temp.cardinality(); i++) {
            uncoloredNodes[i] = temp.nextSetBit(nextSetBit);
            nextSetBit = uncoloredNodes[i]+1;
        }
        return uncoloredNodes;
    }

    public void updateD() {
        for (int i = 0; i < g.getNrVertices(); i++) {
            for (int j = 0; j < currentColorSets.getRows(); j++) {
                if (currentColorSets.getRow(j).get(i)) {
                    if(neighborConflict(i, j, currentColorSets)) {
                        BitSet temp = (BitSet) neighbours.getRow(i).clone();
                        temp.and(currentColorSets.getRow(j));
                        D[i] += temp.cardinality();
                    }
                    break;
                }
            }
        }
    }

    public void updateDNaSwap() {
        for (int i = 0; i < newColorSets.getRows(); i++) {
            BitSet color = (BitSet) newColorSets.getRow(i).clone();
            for (int j = color.nextSetBit(0); j >=0; j = color.nextSetBit(j+1)) {
                // operate on index i here
                BitSet n = (BitSet) neighbours.getRow(j).clone();
                n.and(color);
                tempD[j] += n.cardinality();
                if (j == Integer.MAX_VALUE) {
                    break; // or (i+1) would overflow
                }
            }
        }
    }

    public LinkedList<int[]> findConflictingNodes(BitMatrix b) {
        LinkedList<int[]> conflictingNodes = new LinkedList<>();
        //i = knoop
        //j = kleur toegewezen aan die knoop
        for (int i = 0; i < g.getNrVertices(); i++) {
            for (int j = 0; j < NR_OF_COLORS; j++) {
                if (b.getRow(j).get(i)) {
                    if(neighborConflict(i, j, b)) {
                        int[] temp = new int[2];
                        temp[0] = i;
                        temp[1] = j;
                        conflictingNodes.add(temp);
                    }
                }
            }
        }
        return conflictingNodes;
    }

    public int getColor(int v, BitMatrix b) {
        int k = 0;
        while (k<NR_OF_COLORS && !b.getRow(k).get(v)) {
            k++;
        }
        return k;
    }

    public void operator(int v, int c, int d) {
        //System.out.println("d: "+d);
        //knoop eerst proberen in ongebruikte kleur om conflict op te lossen
        //als geen ongebruikte kleur:
        //knoop swappen met buur waarvan D[knoop] = minimaal
        //daarna operator oproepen op knoop waarmee geswapped werd
        //als knoop geen conflict heeft => keer terug naar vorige knoop die nu andere kleur heeft
        int nc = assignUnusedColor(v);
        if (nc >= 0 && nc < NR_OF_COLORS) {
            //System.out.println("new color: "+nc +" available for node: "+v);
            newColorAssignments++;
            BitSet temp = new BitSet(g.getNrVertices());
            temp.set(v);
            temp.flip(0, g.getNrVertices());
            for (int j = 0; j < newColorSets.getRows(); j++) {
                newColorSets.AND(j, temp);
            }
            newColorSets.set(nc,v);
            //addNodeColorsToList();
        } else {
            swapped = true;
            if (d == 0) swapChains++;
            swapOperations++;
            int[] nonConflictingNeighbours = findConflictingNeighbours(v,c, false);
            if (nonConflictingNeighbours.length != 0) {
                //hier methode implementeren om een buur te kiezen waarmee geswapped zal worden

                //sorteer buren waar knoop geen conflict mee heeft obv waarden in D[]
                //gesorteerdeNCN[0] heeft laagste waarde in D
                int[] gesorteerdeNCN = nonConflictingNeighbours;
                if (nonConflictingNeighbours.length > 1) {
                    gesorteerdeNCN = countSortD(nonConflictingNeighbours);
                }
                int b = gesorteerdeNCN[0];

                //swap kleur van knoop met kleur van 1e knoop in gesorteerdeNCN[]
                int bc = 0;
                while (!newColorSets.getRow(bc).get(b) && bc <= NR_OF_COLORS) {
                    bc++;
                }
                //System.out.println("swap d: "+d + ", ("+v+","+b+")");
                swap(v, b, c, bc);

                //addNodeColorsToList();
                //na swap eerst D updaten
                updateDNaSwap();

                //daarna check of b die nu kleur c heeft (ipv bc), een conflict heeft
                if (neighborConflict(b, c, newColorSets) && d < MAX_DEPTH) {
                    //knoop heeft conflict => roep operator op voor die knoop
                    operator(b, c, d+1);
                }
                //knoop heeft geen conflict => keer terug naar vorige knoop kijk of nieuwe conflicten OF nu wel andere kleur mogelijk is in die knoop
            }
        }
    }

    public int[] countSortD(int[] buren) {
        //buren[] sorteren volgens stijgende waarden van som van conflicten in D[]
        //output[0] heeft laagste waarde van D van alle knopen in buren[]
        int l = buren.length;
        int[] output = new int[l];
        int[] subD = new int[l];
        for (int i = 0; i < l; i++) {
            subD[i] = D[buren[i]] + tempD[buren[i]];
        }

        int maxval = subD[0];

        for (int i = 1; i < l; i++) {
            if (subD[i] > maxval) maxval = subD[i];
        }
        int[] counts = new int[maxval+1];

        for (int j : subD) {
            counts[j]++;
        }

        for (int i = 1; i < counts.length; i++) {
            counts[i] += counts[i-1];
        }

        for (int i = l-1; i >= 0; i--) {
            output[counts[subD[i]]-1] = buren[i];
            counts[subD[i]]--;
        }
        return output;
    }

    public int[] findConflictingNeighbours(int v, int c, boolean conflicts) {
        //if conflicts == true => return list of neighbours that have a conflict with node v
        //if conflicts == false => return list of neighbours that don't have a conflict with node v
        BitSet temp = (BitSet) neighbours.getRow(v).clone();
        //temp = alle buren van v (zonder v zelf)
        if (conflicts) {
            //conflicts = true => we willen alle buren van v waarmee v een conflict heeft
            temp.and(newColorSets.getRow(c));
            //temp = alle buren van v die in dezelfde kleurklasse als v zitten
        } else {
            //conflicts = false => we willen de buren waarmee v geen conflict heeft
            BitSet kleurC = (BitSet) newColorSets.getRow(c).clone();
            kleurC.flip(0, g.getNrVertices());
            //keurC bevat nu alle knopen die niet in kleur c zitten
            temp.and(kleurC);
            //temp bevat alle buren van v die niet in kleurklasse c zitten
        }

        int[] Neighbours = new int[temp.cardinality()];

        int i=0;

        for (int j = temp.nextSetBit(0); j >= 0; j = temp.nextSetBit(j+1)) {
            // operate on index i here
            Neighbours[i] = j;
            if (j == Integer.MAX_VALUE) {
                break; // or (i+1) would overflow
            }
            i++;
        }
        return Neighbours;
    }

    public int assignUnusedColor(int v) {
        //v = node
        //voor elke kleur checken of buren van de knoop in de verzameling van die kleur zitten
        //indien geen buren in verzameling van een knoop => knoop v toewijzen aan de verzameling van die kleur
        //alle kleuren gebruikt door buren van de knoop => return -1
        BitSet buren = (BitSet) neighbours.getRow(v).clone();
        buren.set(v);
        //buren bevat v en zijn buren
        BitSet temp = new BitSet(buren.length());
        temp.or(buren);
        //temp bevat v en zijn buren
        int unusedColor = -1;
        int i = 0;
        while (i < NR_OF_COLORS && unusedColor==-1) {
            temp.or(buren);
            //temp bevat v en zijn buren
            temp.and(newColorSets.getRow(i));
            //temp bevat de knopen v en zijn buren die in kleur klasse i zitten

            if (temp.cardinality() == 0) unusedColor = i;
            //als temp leeg is => kleur i wordt niet gebruikt door v of een van zijn buren
            i++;
        }

        return unusedColor;
    }

    public void swap(int v1, int v2, int c1, int c2) {
        newColorSets.getRow(c1).flip(v1);
        newColorSets.getRow(c1).flip(v2);
        newColorSets.getRow(c2).flip(v1);
        newColorSets.getRow(c2).flip(v2);
    }

    /***Validator***/
    public void validator() {
        for (int i = 0; i < g.getNrVertices(); i++) {
            BitSet adj = (BitSet) neighbours.getRow(i).clone();
            adj.and(bestColoring.getRow(getColor(i, bestColoring)));
            if (adj.cardinality() > 0) {
                //System.out.println("Invalid Solution");
                printColorSets(bestColoring);
                //System.out.print(i +"has conflict with: ");
                for (int j = adj.nextSetBit(0); j >=0 ; j = adj.nextSetBit(j+1)) {
                    System.out.print(j+", ");
                }
            }
        }

        //System.out.println("Valid Solution");
    }

}
