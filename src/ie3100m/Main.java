/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ie3100m;

import Gui.TestGui;
import Logic.BinStatsCalculator;
import Logic.PackingConfigCalculator;
import Logic.QuantitySolver;
import Model.Stats.BinStats;
import Model.Product.Level2_Box;
import Model.Product.Level3_Bin;
import Model.Order;
import Model.Stats.PackingConfig;
import Model.Stats.RankSystem;
import Utils.FileUtils;
import ilog.concert.IloException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Kevin-Notebook
 */
public class Main {

    public static final double MAX_WEIGHT = 30;
    public static final int qty = 1000;
    public static final int length = 190;
    public static final int width = 186;
    public static final int height = 23;
    public static final double weight = 0.06;

    public static void main(String[] args) {
        /**
         * TODO: put order stats as input
         */
        Level2_Box box = new Level2_Box(length, width, height, weight);
        Order order = new Order(box, qty); //in mm and g

        ArrayList<Level3_Bin> binList = new ArrayList<>();

        ArrayList<BinStats> allBinStats = new ArrayList<>();
        ArrayList<PackingConfig> configs = new ArrayList<>();
        
        try {
            binList = FileUtils.loadBinTypes("boxes.csv");

            for (Level3_Bin bin : binList) {
                allBinStats.add(new BinStats(box, bin));
            }
            BinStatsCalculator.setStatsForAllBins(allBinStats);

            for (BinStats binStat : allBinStats) {
                if (binStat.getTotalQuantity() > 0) {
                    configs.add(new PackingConfig(order, binStat));
                }
            }
            PackingConfigCalculator.setAllConfigs(configs, allBinStats);

            ArrayList<PackingConfig> binsByNumbers = new ArrayList<>(configs);
            ArrayList<PackingConfig> binsByVolume = new ArrayList<>(configs);

            PackingConfig bestConfig = determineBestConfig(binsByNumbers, binsByVolume);

            if (bestConfig == null) {
                System.out.println("No suitable config found");
            } else {
                BinStatsCalculator.determineArrangement(bestConfig.getMainBinStats());
                System.out.println("Chosen config:");
                System.out.println(bestConfig);
                TestGui gui = new TestGui(bestConfig.getMainBinStats());
            }
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IloException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    /**
     * Determine the best packing configuration based on the sorting order
     *
     * @param packingConfigs the array of possible packing configurations
     * @return the most desired packing configuration
     */ 
    private static PackingConfig determineBestConfig(ArrayList<PackingConfig> binsByNumbers, ArrayList<PackingConfig> binsByVolume) {
//        System.out.println("testing inside determine");
        Collections.sort(binsByNumbers, (a, b) -> {
            if (a.getTotalBinsInclRemainder() == b.getTotalBinsInclRemainder()) {
                return a.getTotalEmptyVol() <= b.getTotalEmptyVol() ? -1 : 1;
            } else {
                return a.getTotalBinsInclRemainder() - b.getTotalBinsInclRemainder();
            }
        });

        Collections.sort(binsByVolume, (a, b) -> {
            if (a.getMainBinStats().getEmptyVolume() == b.getMainBinStats().getEmptyVolume()) {
                return a.getTotalBinsInclRemainder() <= b.getTotalBinsInclRemainder() ? -1 : 1;
            } else {
                return a.getMainBinStats().getEmptyVolume() - b.getMainBinStats().getEmptyVolume();
            }
        });
//        System.out.println("sorted 2 arrays by num and vol");
        int rankPoints;
        ArrayList<RankSystem> rankBins = new ArrayList<>();

        for (int i = 0; i < binsByNumbers.size(); i++) {
            for (int j = 0; j < binsByVolume.size(); j++) {
                if (binsByNumbers.get(i).getMainBinStats().getBin().getName() == binsByVolume.get(j).getMainBinStats().getBin().getName()) {
                    rankPoints = i + j;
                    RankSystem rank = new RankSystem(binsByVolume.get(j), i, j, rankPoints);
                    rankBins.add(rank);
                }
            }

        }
        Collections.sort(rankBins);
        
        
//        for (RankSystem rank : rankBins) {
//            System.out.println(rank.toString());
//        }
        for (RankSystem rank : rankBins) {
            if (rank.getConfig().getMainBinStats().getTotalQuantity() != 1) {
                return rank.getConfig();
            }            
        }
        return null;
    }

    private static int calcUpperBound(Level2_Box box, Level3_Bin bin) {
        return bin.getBaseArea() / box.getBaseArea();
    }
}
