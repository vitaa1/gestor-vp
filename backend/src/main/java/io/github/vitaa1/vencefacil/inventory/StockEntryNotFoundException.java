package io.github.vitaa1.vencefacil.inventory;

class StockEntryNotFoundException extends RuntimeException {

	StockEntryNotFoundException(long entryId) {
		super("A entrada de estoque " + entryId + " não foi encontrada.");
	}
}
