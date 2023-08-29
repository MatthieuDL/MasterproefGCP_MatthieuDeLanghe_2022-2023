import java.util.BitSet;

public class BitMatrix {
    public BitSet[] bitArray;
    public int rows;
    public int columns;

    public BitMatrix(int r, int c) {
        this.rows = r;
        this.columns = c;
        this.bitArray = new BitSet[r];
        for (int i = 0; i < r; i++) {
            this.bitArray[i] = new BitSet(c);
        }
    }

    public BitMatrix(BitMatrix b) {
        this.rows = b.getRows();
        this.columns = b.getColumns();
        this.bitArray = new BitSet[this.rows];
        for (int i = 0; i < this.rows; i++) {
            this.bitArray[i] = (BitSet) b.getRow(i).clone();
        }
    }

    public int getRows(){
        return this.rows;
    }

    public int getColumns(){
        return this.columns;
    }

    public void setRow(BitSet b, int row) {
        this.bitArray[row] = b;
    }

    public void set(int row, int index) {
        this.bitArray[row].set(index);
    }

    public BitSet getRow(int r) {
        return this.bitArray[r];
    }

    public void XOR(int r, BitSet b) {
        this.bitArray[r].xor(b);
    }

    public void AND(int r, BitSet b) {
        BitSet temp = (BitSet) b.clone();
        this.bitArray[r].and(temp);
    }

    public void removeRow(int r) {
        BitSet[] temp = new BitSet[this.rows];
        for (int i = 0; i < this.rows; i++) {
            if(i<r) {
                temp[i] = (BitSet) this.bitArray[i].clone();
            }
            if (i>r) {
                temp[i-1] = (BitSet) this.bitArray[i].clone();
            }
        }
        this.bitArray = temp;
        this.rows--;
    }
}
