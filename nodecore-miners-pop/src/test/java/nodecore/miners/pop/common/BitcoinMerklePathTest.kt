package nodecore.miners.pop.common

import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import org.junit.Test

class BitcoinMerklePathTest {

    @Test
    fun `should create a valid BitcoinMerklePath with the given path string`() {
        // Given
        val compactFormat = "12:EDB77EEB95390EAA4DAE3BFEC958AEE2BB729CEB3305E16EB4503A2CE41C09B4:A05201273D4BC9E3326F8DA6ADFD700C8DD23CF8F796C49187E38034ACFAFD7E:B6493A8B2F89487C67278D09CF1BF9770733B6E9E948443882CA9DB95DACB8FD:67CCFE77CD7163C0E74198652BAE98E98B94FA0852AD6DD4449F581D22D54147:1E38565FE97FC3973B7CE721F8011B59BC0C3331A8CC68A63BDC10CE20CD79AA:F724DC14859055A7E663F22D40815605083D9489C2E00C8ECAF529B8D92CE854:8A0C5825023D35CEA815F066BF030F740D03D98877E9A9DA6E88DAB69C07CC2D"

        // When
       val merklePath = BitcoinMerklePath(compactFormat)

        // Then
        merklePath.bottomDataIndex shouldBe 12
        merklePath.bottomData shouldBe Utility.hexToBytes("EDB77EEB95390EAA4DAE3BFEC958AEE2BB729CEB3305E16EB4503A2CE41C09B4")
        merklePath.layers.size shouldBe 6
        merklePath.layers[0] shouldBe Utility.hexToBytes("A05201273D4BC9E3326F8DA6ADFD700C8DD23CF8F796C49187E38034ACFAFD7E")
        merklePath.layers[1] shouldBe Utility.hexToBytes("B6493A8B2F89487C67278D09CF1BF9770733B6E9E948443882CA9DB95DACB8FD")
        merklePath.layers[2] shouldBe Utility.hexToBytes("67CCFE77CD7163C0E74198652BAE98E98B94FA0852AD6DD4449F581D22D54147")
        merklePath.layers[3] shouldBe Utility.hexToBytes("1E38565FE97FC3973B7CE721F8011B59BC0C3331A8CC68A63BDC10CE20CD79AA")
        merklePath.layers[4] shouldBe Utility.hexToBytes("F724DC14859055A7E663F22D40815605083D9489C2E00C8ECAF529B8D92CE854")
        merklePath.layers[5] shouldBe Utility.hexToBytes("8A0C5825023D35CEA815F066BF030F740D03D98877E9A9DA6E88DAB69C07CC2D")
    }

    @Test
    fun `the merkle root generated from the given BitcoinMerklePath should be valid`() {
        // Given
        val compactFormat = "12:EDB77EEB95390EAA4DAE3BFEC958AEE2BB729CEB3305E16EB4503A2CE41C09B4:A05201273D4BC9E3326F8DA6ADFD700C8DD23CF8F796C49187E38034ACFAFD7E:B6493A8B2F89487C67278D09CF1BF9770733B6E9E948443882CA9DB95DACB8FD:67CCFE77CD7163C0E74198652BAE98E98B94FA0852AD6DD4449F581D22D54147:1E38565FE97FC3973B7CE721F8011B59BC0C3331A8CC68A63BDC10CE20CD79AA:F724DC14859055A7E663F22D40815605083D9489C2E00C8ECAF529B8D92CE854:8A0C5825023D35CEA815F066BF030F740D03D98877E9A9DA6E88DAB69C07CC2D"

        // When
        val merklePath = BitcoinMerklePath(compactFormat)

        // Then
        merklePath.merkleRoot shouldBe "1C57761E12EC0982653E47CA890804DD91FFD65100866692D1232B191A73EB16"
    }

    @Test
    fun `the compact format generated from the given BitcoinMerklePath should be valid`() {
        // Given
        val compactFormat = "12:EDB77EEB95390EAA4DAE3BFEC958AEE2BB729CEB3305E16EB4503A2CE41C09B4:A05201273D4BC9E3326F8DA6ADFD700C8DD23CF8F796C49187E38034ACFAFD7E:B6493A8B2F89487C67278D09CF1BF9770733B6E9E948443882CA9DB95DACB8FD:67CCFE77CD7163C0E74198652BAE98E98B94FA0852AD6DD4449F581D22D54147:1E38565FE97FC3973B7CE721F8011B59BC0C3331A8CC68A63BDC10CE20CD79AA:F724DC14859055A7E663F22D40815605083D9489C2E00C8ECAF529B8D92CE854:8A0C5825023D35CEA815F066BF030F740D03D98877E9A9DA6E88DAB69C07CC2D"

        // When
        val merklePath = BitcoinMerklePath(compactFormat)

        // Then
        merklePath.compactFormat shouldBe compactFormat
    }

    @Test
    fun `should throw exception if we try to create a BitcoinMerklePath without layers`() {
        // Then
        val exception = shouldThrow<IllegalArgumentException> {
            BitcoinMerklePath(arrayOf(), byteArrayOf(), 12)
        }
        exception.message shouldBe "There must be a nonzero number of layers!"
    }

    @Test
    fun `should throw exception if we try to create a BitcoinMerklePath with wrong layers`() {
        // Given
        val layers = ArrayList<ByteArray>()
        layers.add("A05201273D4BC9E3326F8DA6ADFD700C8DD23CF8F796C49187E38034ACFAFD7".toByteArray())

        // Then
        val exception = shouldThrow<IllegalArgumentException> {
            BitcoinMerklePath(layers.toTypedArray(), byteArrayOf(), 12)
        }
        exception.message shouldBe "Every step of the tree must be a 256-bit number (32-length byte array)!"
    }

    @Test
    fun `should create a valid BitcoinMerklePath with the given parameters`() {
        // Given
        val compactFormat = "12:EDB77EEB95390EAA4DAE3BFEC958AEE2BB729CEB3305E16EB4503A2CE41C09B4:A05201273D4BC9E3326F8DA6ADFD700C8DD23CF8F796C49187E38034ACFAFD7E:B6493A8B2F89487C67278D09CF1BF9770733B6E9E948443882CA9DB95DACB8FD:67CCFE77CD7163C0E74198652BAE98E98B94FA0852AD6DD4449F581D22D54147:1E38565FE97FC3973B7CE721F8011B59BC0C3331A8CC68A63BDC10CE20CD79AA:F724DC14859055A7E663F22D40815605083D9489C2E00C8ECAF529B8D92CE854:8A0C5825023D35CEA815F066BF030F740D03D98877E9A9DA6E88DAB69C07CC2D"

        val layers = ArrayList<ByteArray>()
        layers.add(Utility.hexToBytes("A05201273D4BC9E3326F8DA6ADFD700C8DD23CF8F796C49187E38034ACFAFD7E"))
        layers.add(Utility.hexToBytes("B6493A8B2F89487C67278D09CF1BF9770733B6E9E948443882CA9DB95DACB8FD"))
        layers.add(Utility.hexToBytes("67CCFE77CD7163C0E74198652BAE98E98B94FA0852AD6DD4449F581D22D54147"))
        layers.add(Utility.hexToBytes("1E38565FE97FC3973B7CE721F8011B59BC0C3331A8CC68A63BDC10CE20CD79AA"))
        layers.add(Utility.hexToBytes("F724DC14859055A7E663F22D40815605083D9489C2E00C8ECAF529B8D92CE854"))
        layers.add(Utility.hexToBytes("8A0C5825023D35CEA815F066BF030F740D03D98877E9A9DA6E88DAB69C07CC2D"))
        val bottomData = Utility.hexToBytes("EDB77EEB95390EAA4DAE3BFEC958AEE2BB729CEB3305E16EB4503A2CE41C09B4")
        val bottomDataIndex = 12

        // When
        val merklePath = BitcoinMerklePath(layers.toTypedArray(), bottomData, bottomDataIndex)

        // Then
        merklePath.layers shouldBe layers
        merklePath.bottomData shouldBe bottomData
        merklePath.bottomDataIndex shouldBe bottomDataIndex
        merklePath.compactFormat shouldBe compactFormat
    }

    @Test
    fun `should throw exception if we try to create a BitcoinMerklePath with a path which is too short`() {
        // Given
        val compactFormat = "12:"

        // Then
        val exception = shouldThrow<IllegalArgumentException> {
            BitcoinMerklePath(compactFormat)
        }
        exception.message shouldBe "The compactFormat string must be in the format: \"bottomIndex:bottomData:layer0:...:layerN\""
    }

    @Test
    fun `should throw exception if we try to create a BitcoinMerklePath with an invalid bottomIndex`() {
        // Given
        val compactFormat = "JHEF:EDB77EEB95390EAA4DAE3BFEC958AEE2BB729CEB3305E16EB4503A2CE41C09B4:A05201273D4BC9E3326F8DA6ADFD700C8DD23CF8F796C49187E38034ACFAFD7E:B6493A8B2F89487C67278D09CF1BF9770733B6E9E948443882CA9DB95DACB8FD:67CCFE77CD7163C0E74198652BAE98E98B94FA0852AD6DD4449F581D22D54147:1E38565FE97FC3973B7CE721F8011B59BC0C3331A8CC68A63BDC10CE20CD79AA:F724DC14859055A7E663F22D40815605083D9489C2E00C8ECAF529B8D92CE854:8A0C5825023D35CEA815F066BF030F740D03D98877E9A9DA6E88DAB69C07CC2D"

        // Then
        val exception = shouldThrow<IllegalArgumentException> {
            BitcoinMerklePath(compactFormat)
        }
        exception.message shouldBe "The compactFormat string must be in the format: \"bottomIndex:bottomData:layer0:...:layerN\""
    }

    @Test
    fun `should throw exception if we try to create a BitcoinMerklePath with an invalid bottomData`() {
        // Given
        val compactFormat = "12:EDB77EEB95390EAA4DAE3BFEC958AEE2BB729CEB3305E16EB4503A2CE41C09B:A05201273D4BC9E3326F8DA6ADFD700C8DD23CF8F796C49187E38034ACFAFD7E:B6493A8B2F89487C67278D09CF1BF9770733B6E9E948443882CA9DB95DACB8FD:67CCFE77CD7163C0E74198652BAE98E98B94FA0852AD6DD4449F581D22D54147:1E38565FE97FC3973B7CE721F8011B59BC0C3331A8CC68A63BDC10CE20CD79AA:F724DC14859055A7E663F22D40815605083D9489C2E00C8ECAF529B8D92CE854:8A0C5825023D35CEA815F066BF030F740D03D98877E9A9DA6E88DAB69C07CC2D"

        // Then
        val exception = shouldThrow<IllegalArgumentException> {
            BitcoinMerklePath(compactFormat)
        }
        exception.message shouldBe "The compactFormat string must be in the format: \"bottomIndex:bottomData:layer0:...:layerN\""
    }
}
