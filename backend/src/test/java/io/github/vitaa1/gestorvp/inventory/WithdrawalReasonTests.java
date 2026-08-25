package io.github.vitaa1.gestorvp.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class WithdrawalReasonTests {

	@ParameterizedTest
	@CsvSource({ "SOLD, Venda", "USED, Uso", "DONATED, Doação", "LOST, Perda", "EXPIRED, Vencimento" })
	void exposesTheCanonicalLabel(WithdrawalReason reason, String expectedLabel) {
		assertThat(reason.getLabel()).isEqualTo(expectedLabel);
	}
}
