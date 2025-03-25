public class BitMap {
    private final long[] map;
    private final int totalSlots;

    public BitMap(int totalSlots) {
        this.totalSlots = totalSlots;
        this.map = new long[(this.totalSlots + 63)/64];
    }

    public void set(int index){
        if(index < this.totalSlots){
            this.map[index/64] |= (1L << (index%64));
        }
    }

    public void clear(int index){
        if(index < this.totalSlots){
            this.map[index/64] &= ~(1L << (index%64));
        }
    }

    public boolean isSet(int index){
        return (index < this.totalSlots) && ((this.map[index/64] & (1L << (index%64))) != 0);
    }

    public int findFirstSlot(){
        for (int i = 0; i < this.map.length; i++){
            long inverted = ~this.map[i];
            if(inverted != 0){
                int bit = Long.numberOfTrailingZeros(inverted);
                int index = i*64+bit;
                return (index < this.totalSlots) ? index : -1;
            }
        }
        return -1;
    }
}
