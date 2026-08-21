package io.github.vitaa1.gestorvp.inventory;

class StockEntryNotFoundException extends RuntimeException {

	StockEntryNotFoundException(long entryId) {
		super("A entrada de estoque " + entryId + " não foi encontrada.");
	}
}
