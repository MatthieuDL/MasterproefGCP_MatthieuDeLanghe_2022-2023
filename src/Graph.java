import java.io.File;
import java.io.FileNotFoundException;
import java.util.LinkedList;
import java.util.Scanner;

public class Graph {

    private int nrVertices;
    private int nrEdges;
    private int graphDegree;
    private LinkedList<LinkedList<Integer>> adjList;

    public Graph(String path) throws FileNotFoundException {
        File text = new File(path);
        Scanner sc = new Scanner(text);
        String[] info = sc.nextLine().split(",");
        nrVertices = Integer.parseInt(info[0]);
        adjList = new LinkedList<>();

        for(int i=0; i<nrVertices; i++){
            adjList.add(new LinkedList<Integer>());
        }

        while(sc.hasNextLine()){
            String line= sc.nextLine();
            String[] temp = line.split(":");
            if(temp.length > 1){
                for(String a : temp[1].substring(1).split("\\s+")){
                    adjList.get(Integer.parseInt(temp[0])-1).add(Integer.parseInt(a)-1);
                    if(Integer.parseInt(temp[0])-1 < Integer.parseInt(a)-1) nrEdges++;
                }
            }
        }
        sc.close();
        graphDegree = 0;
        for (int i = 0; i < this.adjList.size(); i++) {
            if (this.adjList.get(i).size() > graphDegree) graphDegree = this.adjList.get(i).size();
        }
    }

    class Edge {
        private final int u;
        private final int v;

        public Edge(int a, int b) {
            this.u = a;
            this.v = b;
        }

        public String toString(){
            return "("+ this.u+","+ this.v+")";
        }

        public int getU() {
            return this.u;
        }

        public int getV() {
            return this.v;
        }
    }

    public int getNrVertices() {
        return this.nrVertices;
    }

    public int getNrEdges() {
        return this.nrEdges;
    }

    public int getGraphDegree() {
        return graphDegree;
    }

    public Edge[] getEdges(){
        Edge[] edges = new Edge[this.nrEdges];
        int j=0;
        for (int i = 0; i<this.adjList.size(); i++){
            LinkedList<Integer> l = this.adjList.get(i);
            for(Integer a : l){
                edges[j] = new Edge(i, a);
                j++;
            }
        }
        return edges;
    }

    public Edge[] getEdgesND() {
        Edge[] edges = new Edge[this.nrEdges];
        int j = 0;
        for (int i = 0; i < this.adjList.size(); i++) {
            LinkedList<Integer> l = this.adjList.get(i);
            for (Integer a: l) {
                if (i < a) {
                    edges[j] = new Edge(i, a);
                    j++;
                }
            }
        }
        return edges;
    }

    public LinkedList<Integer> getNeighbors(int vertex) {
        return adjList.get(vertex);
    }
}
