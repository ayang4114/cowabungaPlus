package cyr7.ir.block;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class BasicBlockSet extends HashSet<BasicBlock> {

    /**
     *
     */
    private static final long serialVersionUID = 7934422314015560748L;
    private LinkedList<BasicBlock> unmarkedBlocks;
    private Map<String, BasicBlock> labelToBlock;

    public BasicBlockSet(List<BasicBlock> blocks) {
        this.addAll(blocks);
        this.unmarkedBlocks = new LinkedList<>(this);
        this.labelToBlock = new HashMap<>();
        this.forEach(b -> {
            b.first.ifPresent(l -> {
                this.labelToBlock.put(l.name(), b);
            });
        });
    }

    public void unmarkBlocks() {
        this.unmarkedBlocks.clear();
        this.forEach(b -> {
            this.unmarkedBlocks.add(b);
        });
    }

    /**
     * Returns the unmarked block that begins with the label {@code label}. If
     * no unmarked block starts with {@code label}, then {@code Empty} is
     * returned.
     *
     * <p>
     * Side Effect: If a block starting with label {@code label} is found, then
     * it is removed from the list of unmarked blocks. Additionally, the
     * succeeding block will be moved to lowest priority, which will affect when
     * the block will appear if an arbitrary block is desired.
     *
     * @param label
     * @return
     */
    public Optional<BasicBlock> getBlock(String label) {
        Iterator<BasicBlock> itr = this.unmarkedBlocks.iterator();
        while (itr.hasNext()) {
            BasicBlock b = itr.next();
            var maybeLabel = b.first;
            if (maybeLabel.isPresent()
                    && maybeLabel.get().name().equals(label)) {
                itr.remove();
                if (itr.hasNext()) {
                    BasicBlock next = itr.next();
                    itr.remove();
                    this.unmarkedBlocks.addLast(next);
                }
                return Optional.of(b);
            }
        }
        return Optional.empty();
    }

    public BasicBlock getAnUnmarkedBlock() {
        return this.unmarkedBlocks.remove();
    }

    public boolean hasUnmarkedBlock() {
        return !this.unmarkedBlocks.isEmpty();
    }


}
