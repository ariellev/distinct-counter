package org.some.thing.counter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * PCSA Estimator
 * @see <a href="http://www.mathcs.emory.edu/~cheung/papers/StreamDB/Probab/1985-Flajolet-Probabilistic-counting.pdf">1985-Flajolet-Probabilistic-counting.pdf</a>
 * @see <a href="https://research.neustar.biz/2013/04/02/sketch-of-the-day-probabilistic-counting-with-stochastic-averaging-pcsa/">probabilistic-counting-with-stochastic-averaging-pcsa</a>
 * @param <T>
 */
public class PCSA<T> implements ICounter<T> {

    private static Logger log = LogManager.getLogger(PCSA.class);

    private int[][] bitmap;
    private static final float PHI = .77351f;
    private static final int NMAP = 256;
    private static final int LENGTH = 32;
    private int nmap;
    private int maxLength;
    private long hashDomain;

    public PCSA(int nmap, int maxLength) {

        if (nmap <= 0)
            throw new IllegalArgumentException("nmap must be greater than 0");

        if (maxLength <= 0)
            throw new IllegalArgumentException("maxLength must be greater than 0");


        this.nmap = nmap;
        this.maxLength = maxLength;
        this.bitmap = new int[nmap][maxLength];
        this.hashDomain = (long) Math.pow(2, maxLength) - 1L;
    }

    public PCSA() {
        this(NMAP, LENGTH);
    }

    /**
     * @param num
     * @return index of the least significant bit b, such that b != 0
     */
    private int LSB(int num) {
        int index = 0;
        while (num > 0 && (num & 1) == 0) {
            num = num >> 1;
            index++;
        }
        return index;
    }

    @Override
    public void add(T element) {
        long h = ((element.hashCode() << 1)) & 0x00000000ffffffffL;
        this.addHash(h);
    }

    @Override
    public void addHash(long hash) {
        long h = hash % this.hashDomain;
        int alpha = (int) (h % nmap);
        int index = LSB((int) (h / nmap));
        bitmap[alpha][index] = 1;
    }


    @Override
    public int cardinality() {

        double sum = 0;

        for (int i = 0; i < nmap; i++) {
            int rank = 0;
            while (bitmap[i][rank] == 1 && rank < maxLength) {
                rank++;
            }
            sum += rank;
        }

        double mean = sum / nmap;
        int card = (int) (nmap * Math.pow(2, mean) / PHI);

        return card;
    }

    @Override
    public double error() {
        return .78 / Math.sqrt(nmap);
    }

    @Override
    public double bias() {
        return 1 + .31 / nmap;
    }

    @Override
    public void clear() {
        for (int[] row : bitmap) {
            Arrays.fill(row, 0);
        }
    }

    @Override
    public boolean isEmpty() {
        int sum = 0;
        int i = 0;

        while (sum == 0 && i < bitmap.length) {
            sum += IntStream.of(bitmap[i]).sum();
            i++;
        }

        return sum == 0;
    }

}
