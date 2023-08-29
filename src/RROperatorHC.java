import java.io.IOException;
import java.util.*;

public class RROperatorHC {

    private Graph g;

    /*****BitMatrices*****/
    private BitMatrix adjMatrix;
    private BitMatrix currentColoring;
    private BitMatrix newColoring;
    private BitMatrix bestColoring;

    /***LinkedLists***/
    LinkedList<Integer> currentConflictedNodes = new LinkedList<>();
    LinkedList<Integer> newConflictedNodes = new LinkedList<>();

    /***Parameters***/
    private int NR_OF_COLORS;
    private int best_NR_OF_COLORS;
    private int[] D;

    private int[][] neighborsPerColor;
    private int[] colorMargin;
    private int[] dSatur;

    private int[][] currentNeighborsPerColor;
    private int[] currentColorMargin;
    private int[] currentDSatur;
    private Random r = new Random(1);

    /***Visualisation***/
    private boolean GRAPHICS = false;
    private boolean GRAPHICSD = false;

    public static void main(String[] args) throws IOException {
        new RROperatorHC("src/Files/seconds/david.txt");
        //new MarginOperatorHC2(args[0]);
    }

    public RROperatorHC(String f) throws IOException {
        colorGraph(f);
    }

    public void colorGraph(String file) throws IOException {
        g = new Graph(file);

        adjMatrix = new BitMatrix(g.getNrVertices(), g.getNrVertices());
        fillAdjMatrix();

        NR_OF_COLORS = g.getGraphDegree() + 1;

        currentColoring = new BitMatrix(NR_OF_COLORS, g.getNrVertices());

        initialColoring();

        best_NR_OF_COLORS = NR_OF_COLORS;

        D = new int[g.getNrVertices()];

        neighborsPerColor = new int[g.getNrVertices()][NR_OF_COLORS];
        colorMargin = new int[g.getNrVertices()];
        dSatur = new int[g.getNrVertices()];

        currentNeighborsPerColor = new int[g.getNrVertices()][NR_OF_COLORS];
        currentColorMargin = new int[g.getNrVertices()];
        currentDSatur = new int[g.getNrVertices()];

        fillColorMargins();
        fillDSatur();

        updateLists();

        while (currentConflictedNodes.size()==0) {
            //System.out.println(NR_OF_COLORS);
            //System.out.println(currentConflictedNodes);
            removeColor();
        }
        System.out.println(NR_OF_COLORS);
        System.out.println(currentConflictedNodes);
        validator(currentColoring);
        newColoring = new BitMatrix(currentColoring);

        localSearch();

        String[] filePath = file.split("/");
        String fileName = filePath[filePath.length-1];

        StringBuilder data = new StringBuilder(fileName+ "," + best_NR_OF_COLORS );
        System.out.println("G,HC X(G)");
        System.out.println(data);

        validator(bestColoring);

        /*
        ,total duration,#swap,#swap improvement,#new color assignment,# worse solutions accepted,#sum conflicts reduction, avg depth of swap chain");
        + "," + duration+ "," + swapOperations +
                "," + swapImprovements + "," + newColorAssignments + "," + timesAcceptedSolutions + ","+ConflictSumReduction + "," + swapOperations/swapChains);
        */
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
    }

    /***Local Search***/
    public void localSearch() {
        double T = 10000;
        double T_MIN = 0.001;

        int iteraties;
        int iteratiesWI;
        double density = 2* (double) g.getNrEdges() /(double)((g.getNrVertices()-1)*g.getNrVertices());
        System.out.println(density);

        int sumPresentConflicts;

        while (T>T_MIN) {
            //System.out.println("T: "+T);
            iteraties = 0;
            iteratiesWI = 0;
            while (iteraties<density*g.getNrVertices()*NR_OF_COLORS && iteratiesWI < 10) {
                updateD(currentConflictedNodes);
                sumPresentConflicts = 0;
                for (int j : currentConflictedNodes) {
                        sumPresentConflicts += D[j];
                }

                int randomConflict = r.nextInt(currentConflictedNodes.size());

                newConflictedNodes = new LinkedList<>(currentConflictedNodes);

                int n = newConflictedNodes.get(randomConflict);
                System.out.println(n);
                //vindt alle knopen waar n een conflict mee heeft
                BitSet adj = (BitSet) adjMatrix.getRow(n).clone();
                int c = getColor(n, newColoring);
                System.out.println(c);
                printColorSets(newColoring);
                adj.and(newColoring.getRow(c));
                int[] conflict = new int[adj.cardinality()+1];
                conflict[0] = n;
                int a = 1;
                for (int i = adj.nextSetBit(0); i >=0 ; i = adj.nextSetBit(i+1)) {
                    conflict[a] = i;
                    a++;
                    if (i ==Integer.MAX_VALUE) break;
                }

                //operator oproepen
                System.out.println(Arrays.toString(conflict));
                operator(conflict);

                int newSumConflicts = 0;
                for (int newConflictedNode : newConflictedNodes) {
                    newSumConflicts += D[newConflictedNode];
                }

                if(newSumConflicts<sumPresentConflicts) {
                    iteratiesWI = 0;
                    currentColoring = new BitMatrix(newColoring);
                    currentConflictedNodes = new LinkedList<>(newConflictedNodes);
                    updateLists();
                }
                else if (Math.exp((sumPresentConflicts-newSumConflicts)/T)> r.nextDouble()) {
                    //timesAcceptedSolutions++;
                    iteratiesWI = 0;
                    currentColoring = new BitMatrix(newColoring);
                    currentConflictedNodes = new LinkedList<>(newConflictedNodes);
                    updateLists();
                }
                else {
                    newColoring = new BitMatrix(currentColoring);
                    newConflictedNodes = new LinkedList<>(currentConflictedNodes);
                    iteratiesWI++;
                    restoreLists();
                }

                if (newSumConflicts == 0) {
                    System.out.println(NR_OF_COLORS);
                    //if (GRAPHICS || GRAPHICSD) printColorRemoved(System.nanoTime() - timeStart);
                    //printColorSets(currentColorSets);

                    /*if(GRAPHICS) {
                        long currTime = System.nanoTime() - timeStart;
                        times.add(currTime);
                        values.add((long) sumPresentConflicts);
                    }*/
                    //kleur verwijderen waarvoor som van conflict counters (D) het laagst is
                    if (NR_OF_COLORS==6) printColorSets(currentColoring);

                    if (NR_OF_COLORS==4) {
                        System.out.println(newConflictedNodes);
                    }
                    removeColor();
                    //na verwijderen van kleur bevat currentColorSets opnieuw conflicten
                }
                iteraties++;
            }
            T*=0.999;
        }
    }

    /***Operator***/

    public void operator(int[] conflict) {
        //conflict[] bevat alle knopen die een conflict vormen (soms 2 knopen, soms 3 knopen kan ketting of driehoek zijn...)

        //verzamel alle ontkleurde knopen
        LinkedList<Integer> uncoloredNodes = new LinkedList<>();

        //ontkleur knopen in conflict[] & pas marges aan van buren van deze knopen
        for (int k : conflict) {
            uncolorNode(k);
            uncoloredNodes.add(k);
        }

        for (int k : conflict) {
            BitSet adj = new BitSet(g.getNrVertices());
            adj.or(adjMatrix.getRow(k));

            //ontkleur alle buren die nu een marge > 0 hebben
            for (int j = adj.nextSetBit(0); j >= 0; j = adj.nextSetBit(j + 1)) {
                if (colorMargin[j] > 0 && !uncoloredNodes.contains(j)) {
                    uncolorNode(j);
                    uncoloredNodes.add(j);
                }
            }
        }

        for (Integer uncoloredNode : uncoloredNodes) {
            System.out.println("node: "+uncoloredNode+" margin: "+colorMargin[uncoloredNode]);
        }
        //sorteer ontkleurde knopen op Marge (kleinste eerst), saturatiegraad (grootste eerst) dan random
        uncoloredNodes = sortUncoloredNodes(uncoloredNodes);

        //neem de eerste knoop af en wijs kleur toe
        while (!uncoloredNodes.isEmpty()) {
            int v = uncoloredNodes.getFirst();

            int c;
            //als colorMargin[v] > 1
            if (colorMargin[v] == 1) { //knoop kan maar 1 kleur krijgen => toewijzen
                c = 0;
                while (neighborsPerColor[v][c] != 0) {
                    c++;
                }
            } else {//knoop kan meerdere kleuren krijgen
                //bepaal kleur(en) waarvoor minste 0'en verdwijnen in burenPerKleur matrix van elke buur
                //of colorMargin[v]==0
                //dan kies kleur waarvoor minste 0'en verdwijnen in burenPerKleur
                //als meerdere kleuren evenveel 0 verwijderen => kies die waarvoor minste buren, als evenveel => kies hoogste
                c = bestColor(v);
            }

            //wijs gekozen kleur toe aan knoop en update marges van buren
            colorNode(v, c, newColoring, newConflictedNodes);
            //verwijder gekleurde knoop uit uncolored nodes lijst
            uncoloredNodes.remove(uncoloredNodes.lastIndexOf(v));
            //kijk of sortering moet worden aangepast
            updateUncoloredNodes(uncoloredNodes, v, c);
        }
    }

    public void updateUncoloredNodes(LinkedList<Integer> uncoloredNodes, int v, int c) {
        BitSet temp = new BitSet(g.getNrVertices());

        for (int i = 0; i < NR_OF_COLORS; i++) {
            temp.or(newColoring.getRow(i));
        }

        temp.flip(0, g.getNrVertices());
        System.out.println(temp);
        temp.and(adjMatrix.getRow(v));
        System.out.println(temp.cardinality());

        for (int n: uncoloredNodes) {
            System.out.print(n+", ");
        }
        //temp bevat nu alle ongekleurde buren van v
        //voor deze buren positie in lijst herbekijken
        for (int i = temp.nextSetBit(0); i >= 0; i= temp.nextSetBit(i+1)) {
            int currentIndex = uncoloredNodes.lastIndexOf(i);
            System.out.println(i);
            printColorSets(newColoring);
            System.out.println(currentIndex);
            if (uncoloredNodes.size()>0) {
                int newIndex = 0;
                while (colorMargin[uncoloredNodes.get(newIndex)] < colorMargin[i]) {
                    newIndex++;
                }

                while (dSatur[uncoloredNodes.get(newIndex)] > dSatur[i] && newIndex <= currentIndex) {
                    newIndex++;
                }
                System.out.println(currentIndex);
                while (currentIndex > newIndex) {
                    int n = uncoloredNodes.get(currentIndex-1);
                    uncoloredNodes.set(currentIndex, n);
                    currentIndex--;
                }
                System.out.println(newIndex+", "+currentIndex);
                uncoloredNodes.set(currentIndex, i);
            }
        }
    }

    public void colorNode(int v, int c, BitMatrix coloring, LinkedList<Integer> conflictedNodes) {
        coloring.getRow(c).set(v, true);
        BitSet adj = (BitSet) adjMatrix.getRow(v).clone();
        //pas de info aan voor alle buren van v
        for (int i = adj.nextSetBit(0); i >= 0; i = adj.nextSetBit(i+1)) {

            if (neighborsPerColor[i][c] == 0 && !coloring.getRow(c).get(i) && colorMargin[i]>0) {
                assert  currentColorMargin[i] > 0 && currentColorMargin[i] < NR_OF_COLORS;
                colorMargin[i]--;
            }
            if (neighborsPerColor[i][c] == 0 && coloring.getRow(c).get(i)) {
                conflictedNodes.add(i);
                if (!conflictedNodes.contains(v)) conflictedNodes.add(v);
            }
            neighborsPerColor[i][c]++;
            dSatur[i]++;
        }
    }

    public int bestColor(int v) {
        //return kleur waarvoor minste 0 in neighborspercolor van ongekleurde buren verdwijnen
        BitSet adj = (BitSet) adjMatrix.getRow(v).clone();
        BitSet temp = new BitSet(g.getNrVertices());
        for (int i = 0; i < NR_OF_COLORS; i++) {
            temp.or(newColoring.getRow(i));
        }
        temp.flip(0, g.getNrVertices());
        temp.and(adj);
        //temp bevat nu alle ongekleurde buren van v
        int[] colors = new int[NR_OF_COLORS];
        for (int i = temp.nextSetBit(0); i >=0 ; i = temp.nextSetBit(i+1)) {
            for (int j = 0; j < NR_OF_COLORS; j++) {
                if (neighborsPerColor[i][j] == 0) colors[j]++;
            }
            if (i == Integer.MAX_VALUE) break;
        }
        int min = colors[0];
        int c = 0;
        for (int i = 1; i < colors.length; i++) {
            if (colors[i]<min) {
                min = colors[i];
                c = i;
            }
        }

        int minsteBuren = Integer.MAX_VALUE;

        BitSet temp2 = (BitSet) adjMatrix.getRow(v).clone();
        for (int i = 0; i < colors.length; i++) {
            if (colors[i] == min) {
                temp2.and(newColoring.getRow(i));
                if (temp2.cardinality()<minsteBuren) {
                    minsteBuren = temp2.cardinality();
                    c = i;
                }
                temp2.or(adjMatrix.getRow(v));
            }
        }

        return c;
    }

    public void uncolorNode(int v) {
        //verwijder knoop v uit BitMatrix newColoring
        int c = 0;
        while (!newColoring.getRow(c).get(v)) {
            c++;
        }

        newColoring.getRow(c).set(v, false);

        //vraag buren van v op
        BitSet adj = (BitSet) adjMatrix.getRow(v).clone();

        if (newConflictedNodes.contains(v)) newConflictedNodes.remove(newConflictedNodes.lastIndexOf(v));

        //pas lijsten aan van buren van v
        for (int j = adj.nextSetBit(0); j >= 0; j = adj.nextSetBit(j+1)) {
            updateListsAfterUncoloringNode(j);
            if (newConflictedNodes.contains(j) && neighborsPerColor[j][c]==0 && newColoring.getRow(c).get(j)) newConflictedNodes.remove(newConflictedNodes.lastIndexOf(j));
        }
    }

    public LinkedList<Integer> sortUncoloredNodes(LinkedList<Integer> uncoloredNodes) {
        //lijst van ints sorteren op
        //stijgende marge => eerst deellijsten maken van knopen met zelfde marges
        //dalende saturatiegraad => deellijsten sorteren op stijgende saturatiegraad
        //stijgende graad => als toch nog gelijke stand => knoop met kleinste graad eerst
        //nog geen onderscheid dan random

        //eerst buckets maken per marge
        //elke bucket sorteren door te verdelen in buckets op saturatie graad deze subbucket sorteren op graad
        //elke subbucket legen in originele bucket zodat deze gesorteerde knopen met een bep marge bevatten
        //buckets legen in originele lijst

        LinkedList<Integer> sorted = new LinkedList<>();

        LinkedList<LinkedList<Integer>> sortByMargin= new LinkedList<>();
        for (int i = 0; i <= NR_OF_COLORS; i++) {
            sortByMargin.add(new LinkedList<>());
        }

        for (Integer uncoloredNode : uncoloredNodes) {
            sortByMargin.get(colorMargin[uncoloredNode]).add(uncoloredNode);
        }

        for (int i = 0; i < NR_OF_COLORS; i++) {
            if (!sortByMargin.get(i).isEmpty()) {
                sortByMargin.set(i, new LinkedList<>(sortByDSat(sortByMargin.get(i))));
                sorted.addAll(sortByMargin.get(i));
            }
        }
        return sorted;
    }

    public LinkedList<Integer> sortByDSat(LinkedList<Integer> sortedByMargin) {
        //lijst sortedByMargin bevat knopen met dezelfde marge
        //moet gesorteerd worden op saturatiegraad mbv countsort

        int[] satD = new int[sortedByMargin.size()];
        for (int i = 0; i < sortedByMargin.size(); i++) {
            for (int j = 0; j < NR_OF_COLORS; j++) {
                satD[i] += neighborsPerColor[sortedByMargin.get(i)][j];
            }
        }

        int max = satD[0];
        for (int i = 1; i < satD.length; i++) {
            if (satD[i] > max) max = satD[i];
        }

        int[] counts = new int[max+1];

        for (int j : satD) {
            counts[j]++;
        }

        for (int i = 1; i < counts.length; i++) {
            counts[i] += counts[i-1];
        }

        int[] output = new int[sortedByMargin.size()];
        for (int i = sortedByMargin.size()-1; i >= 0; i--) {
            output[counts[satD[i]]-1] = sortedByMargin.get(i);
            counts[satD[i]]--;
        }

        LinkedList<Integer> sortedByDSat = new LinkedList<>();

        for (int j : output) {
            sortedByDSat.add(j);
        }

        return sortedByDSat;
    }

    /***Initial Coloring***/

    public void initialColoring() {
        //genereer initiële kleuring mbv greedy algoritme

        //1e knoop (0) toewijzen aan 1e kleurklasse (0)
        currentColoring.getRow(0).set(0, true);

        for (int v=1; v< g.getNrVertices(); v++) {
            int c = 0;

            currentColoring.getRow(c).set(v, true);
            //assign(v, c, currentColoring);

            while (neighborConflict(v, c, currentColoring)){
                currentColoring.getRow(c).set(v, false);
                c++;
                currentColoring.getRow(c).set(v, true);
                if(c > NR_OF_COLORS) NR_OF_COLORS = c;
            }
        }
        removeInitialRedundantSets(currentColoring);
    }

    public boolean neighborConflict(int v, int c, BitMatrix b) {
        //check of een de knopen in de kleurklasse van v buren zijn van v
        BitSet temp = (BitSet) adjMatrix.getRow(v).clone();
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

    /***Remove Color***/

    public void removeColor() {
        bestColoring = new BitMatrix(currentColoring);
        best_NR_OF_COLORS = NR_OF_COLORS;
        validator(bestColoring);
        //vindt kleur klasse waarvoor minste conflict counters
        int c = leastConflictCounters();
        BitSet uncoloredNodes = (BitSet) currentColoring.getRow(c).clone();
        currentColoring.removeRow(c);
        NR_OF_COLORS--;
        recolorNodes(uncoloredNodes);
        updateListsAfterRemoval();
        newColoring = new BitMatrix(currentColoring);
        //printColorSets(newColoring);
    }

    public int leastConflictCounters() {
        //kleur klasse vinden waar voor de som van de geschiedenis van conflicten van knopen in die klasse de kleinste is
        int row=-1;
        int minSum = Integer.MAX_VALUE;
        for (int i = 0; i < currentColoring.getRows(); i++) {
            BitSet temp = currentColoring.getRow(i);
            int sum = 0;
            for (int j = 0; j < temp.cardinality(); j++) {
                int v = temp.nextSetBit(j);
                sum += D[v];
            }

            if(sum<minSum) {
                row = i;
                minSum = sum;
            }
        }

        return row;
    }

    public void recolorNodes(BitSet uncoloredNodes) {
        //ongekleurde knopen opnieuw verdelen over overige kleurklassen zodat aantal conflicten (voor die knopen) minimaal is
        for (int i = uncoloredNodes.nextSetBit(0); i >= 0 ; i = uncoloredNodes.nextSetBit(i+1)) {
            int[] conflicts = new int[NR_OF_COLORS];
            int new_color = 0;
            int min_conflicts = Integer.MAX_VALUE;

            //per kleur set bepalen hoeveel buren erin zitten
            //ongekleurde knoop toewijzen aan kleurset die minste aantal buren bevat => minst nieuwe conflicten
            //indien meedere kleuren zelfde aantal buren => laagste kleur

            BitSet temp = new BitSet(g.getNrVertices());
            for (int j = 0; j < NR_OF_COLORS; j++) {
                temp.or(adjMatrix.getRow(i));
                temp.and(currentColoring.getRow(j));
                conflicts[j] = temp.cardinality();
                if (temp.cardinality() < min_conflicts) {
                    min_conflicts = temp.cardinality();
                    new_color = j;
                }
            }

            //knoop wordt toegevoegd aan kleur met minste conflicten
            //als minste conflicten >0 dan verzamel alle knopen die nu een conflict hebben door deze toevoeging
            // (allemaal buren van de knoop die werd toegevoegd)
            //deze knopen toevoegen in verzameling van conflicten

            currentColoring.getRow(new_color).set(i, true);
            BitSet newConflicts = new BitSet(g.getNrVertices());
            newConflicts.or(currentColoring.getRow(new_color));
            newConflicts.and(adjMatrix.getRow(i));

            if (newConflicts.cardinality()>0) {
                currentConflictedNodes.add(i);
                for (int j = newConflicts.nextSetBit(0); j >= 0; j = newConflicts.nextSetBit(j+1)) {
                    if (!currentConflictedNodes.contains(j)) currentConflictedNodes.add(j);
                }
            }

            //colorNode(i, new_color, currentColoring, currentConflictedNodes);

            if (i == Integer.MAX_VALUE) {
                break;
            }
        }
    }

    /***Conflict Counters D***/
    public void updateD(LinkedList<Integer> currentConflictedNodes) {
        for (int n : currentConflictedNodes) {
            BitSet adj = new BitSet(g.getNrVertices());
            adj.or(adjMatrix.getRow(n));
            adj.and(currentColoring.getRow(getColor(n, currentColoring)));
            D[n] += adj.cardinality();
        }
    }

    public void updateLists() {
        for (int i = 0; i < g.getNrVertices(); i++) {

            assert  colorMargin[i] >= 0 && colorMargin[i] < NR_OF_COLORS;
            currentColorMargin[i] = colorMargin[i];
            assert dSatur[i] >= 0;
            currentDSatur[i] = dSatur[i];

            if (currentNeighborsPerColor[i].length > neighborsPerColor[i].length) {
                currentNeighborsPerColor[i] = new int[NR_OF_COLORS];
            }

            for (int j = 0; j < NR_OF_COLORS; j++) {
                assert neighborsPerColor[i][j]>= 0;
                currentNeighborsPerColor[i][j] = neighborsPerColor[i][j];
            }
        }
    }

    public void restoreLists() {
        for (int i = 0; i < g.getNrVertices(); i++) {
            assert  currentColorMargin[i] >= 0 && currentColorMargin[i] < NR_OF_COLORS;
            colorMargin[i] = currentColorMargin[i];
            dSatur[i] = currentDSatur[i];
            for (int j = 0; j < NR_OF_COLORS; j++) {
                neighborsPerColor[i][j] = currentNeighborsPerColor[i][j];
            }
        }
    }

    public void updateListsAfterRemoval() {
        fillColorMargins();
        fillDSatur();
    }

    public void updateListsAfterUncoloringNode(int v) {
        updateNPC(v, newColoring);
        updateMargin(v, newColoring);
        updateDSatur(v, newColoring);
    }

    /***Degree of Saturation***/
    public void updateDSatur(int v, BitMatrix b) {
        BitSet adj = new BitSet(g.getNrVertices());
        for (int i = 0; i < NR_OF_COLORS; i++) {
            adj.or(b.getRow(i));
        }
        adj.and(adjMatrix.getRow(v));
        dSatur[v] = adj.cardinality();
    }

    public void fillDSatur() {
        for (int i = 0; i < g.getNrVertices(); i++) {
            currentDSatur[i] = adjMatrix.getRow(i).cardinality();
        }
    }

    /***Neighbors Per Color***/
    public void updateNPC(int v, BitMatrix b) {
        for (int i = 0; i < NR_OF_COLORS; i++) {
            BitSet adj = new BitSet(g.getNrVertices());
            adj.or(adjMatrix.getRow(v));
            adj.and(b.getRow(i));
            neighborsPerColor[v][i] = adj.cardinality();
        }
    }

    public void fillNeighborsPerColor() {
        for (int i = 0; i < g.getNrVertices(); i++) {
            currentNeighborsPerColor[i] = new int[NR_OF_COLORS];
            for (int j = 0; j < NR_OF_COLORS; j++) {
                BitSet temp = new BitSet(g.getNrVertices());
                //alle buren opnieuw in bitset verzamelen
                temp.or(adjMatrix.getRow(i));
                //per kleur kijken hoeveel buren erin zitten
                temp.and(currentColoring.getRow(j));
                //bijhouden hoeveel buren in elke kleur zitten
                currentNeighborsPerColor[i][j] = temp.cardinality();
            }
        }
    }

    /***Color Margins***/

    public void updateMargin(int v, BitMatrix b) {
        int margin = 0;
        for (int i = 0; i < NR_OF_COLORS; i++) {
            if(neighborsPerColor[v][i] == 0 && !b.getRow(i).get(v)) margin++;
        }
        colorMargin[v] = margin;
    }

    public void fillColorMargins() {
        fillNeighborsPerColor();
        for (int i = 0; i < g.getNrVertices(); i++) {
            int margin = 0;
            for (int j = 0; j < NR_OF_COLORS; j++) {
                if (currentNeighborsPerColor[i][j] == 0 && !currentColoring.getRow(j).get(i)) margin++;
            }
            currentColorMargin[i] = margin;
        }
    }

    public int getColor(int v, BitMatrix b) {
        int k = 0;
        while (k<NR_OF_COLORS && !b.getRow(k).get(v)) {
            k++;
        }
        return k;
    }

    /***Adjacency Matrix***/

    public void fillAdjMatrix() {
        for (int i = 0; i < g.getNrVertices(); i++) {
            LinkedList<Integer> adjList = g.getNeighbors(i);
            BitSet temp = new BitSet(g.getNrVertices());
            for (Integer integer : adjList) {
                if (integer != i) temp.set(integer);
            }
            adjMatrix.setRow(temp, i);
        }
    }

    /***Print Coloring***/
    public static void printColorSets(BitMatrix b) {
        for (int i = 0; i < b.getRows(); i++) {
            System.out.println(b.getRow(i));
        }
        System.out.println("#########################");
    }

    /***Validator***/
    public void validator(BitMatrix b) {
        boolean valid = true;
        for (int i = 0; i < g.getNrVertices(); i++) {
            BitSet adj = (BitSet) adjMatrix.getRow(i).clone();
            adj.and(b.getRow(getColor(i, b)));
            if (adj.cardinality() > 0) {
                valid = false;
                System.out.print(i +" has conflict with: ");
                for (int j = adj.nextSetBit(0); j >=0 ; j = adj.nextSetBit(j+1)) {
                    System.out.print(j+", ");
                }
                System.out.println();
            }
        }
        if (valid) {
            System.out.println("Valid Solution with: "+b.getRows()+" colors");
        } else {
            System.out.println("Invalid Solution");
            printColorSets(b);
        }

    }
}