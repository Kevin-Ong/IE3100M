/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Model.Stats;

import Logic.BinStatsCalculator;
import Model.Order;
import Model.Product.Level3_Bin;


public class PackingConfig /*implements Comparable<PackingConfig>*/ {
    private final Order order;
    private final BinStats mainBinStats;
    

    private Level3_Bin lastBin;

    private long totalEmptyVol;
    private int totalBins;
    
    public PackingConfig (Order order, BinStats mainBinStats) {
        this.order = order;
        this.mainBinStats = mainBinStats;
    }
    
    public Order getOrder() {
        return this.order;
    }
    
    public BinStats getMainBinStats() {
        return this.mainBinStats;
    }

    public int getTotalBins() {
        return this.totalBins;
    }

    public int getTotalBinsInclRemainder() {
        return this.getRemainderBoxes() == 0 ? this.totalBins : this.totalBins + 1;
    }
    
    public int getNumberOfMainBins() {
        return this.order.getQuantity() / this.mainBinStats.getTotalQuantity();
    }

    public int getRemainderBoxes() {
        return this.order.getQuantity() % this.mainBinStats.getTotalQuantity();
    }

    public Level3_Bin getLastBin() {
        return this.lastBin;
    }

    public long getTotalEmptyVol() {
        return this.totalEmptyVol;
    }
    
    public void setAttributes(Level3_Bin lastBin, int totalBins) {
        setLastBin(lastBin);
        setTotalBins(totalBins);
        
        if (lastBin == null) {//no remainder bins, either only 1 bin is needed or the boxes can fit into the main bins with no leftover
            if (totalBins == 1) { //case where only 1 bin is needed
                setTotalEmptyVol((long) this.mainBinStats.getBin().getTrimmedVolume(BinStatsCalculator.getBuffer()) - (this.order.getQuantity() * this.order.getBox().getVolume()));
            } else { //case where there are no leftovers
                setTotalEmptyVol((long) totalBins * this.mainBinStats.getEmptyVolume());
            }
        } else {
            setTotalEmptyVol(((long) totalBins * this.mainBinStats.getEmptyVolume()) + ((long) lastBin.getTrimmedVolume(BinStatsCalculator.getBuffer()) - (this.getRemainderBoxes() * this.order.getBox().getVolume())));
        }
    }
    
    private void setLastBin(Level3_Bin lastBin) {
        this.lastBin = lastBin;
    }
    
    private void setTotalBins(int totalBins) {
        this.totalBins = totalBins;
    }
    
    private void setTotalEmptyVol(long totalEmptyVol) {
        this.totalEmptyVol = totalEmptyVol;
    }
    
    public String display() {
        String mainBinInfo = "Main Shipping Carton box:\n" +
                "Name:\t\t" + this.mainBinStats.getBin().getFullName() + "\n" +
                "Dimensions:\t\t" + this.mainBinStats.getBin().getDimensions() + "\n" +
                "Quantity:\t\t" + this.getNumberOfMainBins() + "\n" +
                "Layers perbox:\t" + this.mainBinStats.getBin().getHeight() / this.mainBinStats.getBox().getHeight();
        
        String remBinInfo;
        if (lastBin != null) {
            remBinInfo = "Shipping Carton box for remaining products:\n" +
                    "Name: " + this.lastBin.getFullName();
        } else {
            remBinInfo = "No remaining shipping box needed";
        }
        
        return mainBinInfo + "\n\n" + remBinInfo;
    }

    @Override
    public String toString() {

        String info = "Bin type: " + this.mainBinStats.getBin().toString() 
                + " Boxes per Bin: " + this.mainBinStats.getTotalQuantity()
                + " Bins Needed: " + totalBins + " empty vol: " + this.mainBinStats.getEmptyVolume();
        
        return info + (lastBin == null ? " Last Bin not needed" : "\n" + lastBin.toString() + " to contain remaining " + this.getRemainderBoxes() + " boxes");
    }

}
