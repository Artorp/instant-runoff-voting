package no.artorp.instantrunoffvoting.client;

import javafx.collections.ObservableList;
import javafx.scene.control.ListCell;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;

public class DragableListCell extends ListCell<String> {

	public DragableListCell() {
		ListCell<String> thisCell = this;
		
		setOnDragDetected(me -> {
			if (getItem() == null)
				return;
			
			Dragboard dragboard = startDragAndDrop(TransferMode.MOVE);
			ClipboardContent content = new ClipboardContent();
			content.putString(getItem().toString());
			dragboard.setDragView(this.snapshot(null, null));
			dragboard.setContent(content);
			me.consume();
		});
		
		setOnDragOver(de -> {
			if (de.getGestureSource() != thisCell &&
					de.getDragboard().hasString())
				de.acceptTransferModes(TransferMode.MOVE);
			
			de.consume();
		});
		
		setOnDragEntered(de -> {
			if (de.getGestureSource() != thisCell &&
					de.getDragboard().hasString())
				setOpacity(0.2);
		});
		
		setOnDragExited(de -> {
			if (de.getGestureSource() != thisCell &&
					de.getDragboard().hasString())
				setOpacity(1);
		});
		
		setOnDragDropped(de -> {
			if (getItem() == null)
				return;
			
			Dragboard dragboard = de.getDragboard();
			boolean success = false;
			
			if (dragboard.hasString()) {
				ObservableList<String> items = getListView().getItems();
				int this_index = items.indexOf(getItem());
				int other_index = -1;
				for (int i = 0; i < items.size(); i++) {
					String c = items.get(i);
					if (c.equals(dragboard.getString())) {
						other_index = i;
						break;
					}
				}
				if (other_index != -1) {
					// Swap the items
					String temp = items.get(other_index);
					items.set(other_index, getItem());
					items.set(this_index, temp);
					getListView().getSelectionModel().select(this_index);
					
					success = true;
				}
			}
			de.setDropCompleted(success);
			de.consume();
		});
		
		setOnDragDone(DragEvent::consume);
	}

	@Override
	protected void updateItem(String item, boolean empty) {
		super.updateItem(item, empty);
		
		if (item == null || empty) {
			setText(null);
		} else {
			setText(item);
		}
	}
	
	

}
