package com.wheelpicker;
final class LoopRunnable implements Runnable {

    final LoopView loopView;

    LoopRunnable(LoopView loopview) {
        super();
        loopView = loopview;

    }

    @Override
    public final void run() {
        LoopListener listener = loopView.loopListener;
        int selectedItem = LoopView.getSelectedItem(loopView);
        if(selectedItem < loopView.arrayList.size() && selectedItem >= 0){
            loopView.arrayList.get(selectedItem);
            listener.onItemSelect(loopView, selectedItem);
        }
    }
}
