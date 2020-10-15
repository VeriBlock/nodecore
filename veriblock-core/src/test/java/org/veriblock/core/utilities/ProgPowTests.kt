package org.veriblock.core.utilities

import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test
import org.veriblock.core.Context
import org.veriblock.core.params.defaultTestNetParameters
import org.veriblock.core.utilities.extensions.asHexBytes
import org.veriblock.sdk.services.SerializeDeserializeService
import java.security.Security
import kotlin.system.measureTimeMillis

@Ignore
class ProgPowTests {

    @Test
    fun testProgPowHash() {
        Context.create(defaultTestNetParameters)
        Security.addProvider(BouncyCastleProvider())
        val headerHex = "000D532B00020F2F1A55A6523A39EB7DC08CFD0B88C6E6AC5C79FB12D2F9E527B45D57FE18D30532FB0694DFA136EAAA2594D31F5F6608FB04257C501003A12C2C"
        val header = headerHex.asHexBytes()
        val height = BlockUtility.extractBlockHeightFromBlockHeader(header)
        val hash = BlockUtility.hashProgPowBlock(header, height)
        Assert.assertEquals("00000000AC2178C652A1051DB63637340BB251424E9DCE8C", hash)
    }

    @Ignore
    @Test
    fun performanceTest() {
        val blocksString = """
            000D5AD30002CBB1B687FA791BC1951930C49C5C31AF725D249C8CE4DB5DF468002F8582E6B67E5A5BF65ECDC38FA4B09714C8985F6750680427E4D420026A399E
            000D5AD40002530956716D6B835734B090439C5C31AF725D249C8CE4DB5DF468002F85828F3F63CE7441A46AA73C1FFCF2CD25455F675070042863E4300100B699
            000D5AD50002C180DE712F3BF99742BD8DDE9C5C31AF725D249C8CE4DB5DF468002F8582207938CE6279BC042D5D5C9D3097A8D25F675082042903A83001F3A528
            000D5AD60002A787BAE8AACAE8B1A9D619D6712F3BF99742BD8DDE9C5C31AF725D249C8C6379452579399A9DE33496426E44807F5F67509D04295CAF10029CC198
            000D5AD700024EFEDFD42846255C81C32C39712F3BF99742BD8DDE9C5C31AF725D249C8C0DF16341AAA7BEAE37FC97EA016C76DD5F6750A6042971B2200926EAD5
            000D5AD80002B51DA638B0CD42D99CF4AF27712F3BF99742BD8DDE9C5C31AF725D249C8C5D93F04F5F52643C8250746BA9E495EC5F67511A042A1369100024ED4B
            000D5AD90002FAB0376F1407E2DFD82F223B712F3BF99742BD8DDE9C5C31AF725D249C8CA96843ECF5656CD3B7BCD7828650A5005F67511B04279F78100714C574
            000D5ADA000212D335E3060C67CCDE2358AA712F3BF99742BD8DDE9C5C31AF725D249C8C9EE8D96A65CC541B435A8D090B6B3BA35F675130042872E8100150D70D
            000D5ADB0002DE07FE5DB579111A017FC7D8712F3BF99742BD8DDE9C5C31AF725D249C8C23B261471997022CF9C48DAC2E454E465F67514A0428AFDF005D815B41
            000D5ADC00029E8692A09BE6DA28C971FC55712F3BF99742BD8DDE9C5C31AF725D249C8C72F5157F7B03A561C76352FAB14C8DBC5F67514C0428CBC3100B74683F
            000D5ADD0002347D9EBDA1698C76D0DEA26A712F3BF99742BD8DDE9C5C31AF725D249C8CFEF48FFEBA29AC78D89E5B182389C7D25F6751A6042993C81003746C7E
            000D5ADE0002F14FB4A82F27794D73C8B35B712F3BF99742BD8DDE9C5C31AF725D249C8CCFB365BC0437CE975BC6F34ED6867C205F6751C70427DEDB008BD02D93
            000D5ADF00020868FDAD192D12D18AF4CB57712F3BF99742BD8DDE9C5C31AF725D249C8C5F4A324458A3E078B85920379CA6F00A5F6751CF0427CF4C30035A497D
            000D5AE00002BA1E5509A4B1D8A0DE6557CD712F3BF99742BD8DDE9C5C31AF725D249C8C9855A204D74870C2E61ABA87F78A7DFC5F6751EE042869E92004272EFD
            000D5AE10002C304912C0FF127A529D039E2712F3BF99742BD8DDE9C5C31AF725D249C8C995FD016399B6C96544CDDB926DE749B5F67520F04285EA8100230CF14
            000D5AE200020DB674A91B273B32C14A40C0712F3BF99742BD8DDE9C5C31AF725D249C8CAD922365EC8AFBA36D78CC90480E4D445F67522B042847F900B2A41931
            000D5AE30002B45DB01A5AEC9C02DA32EE74712F3BF99742BD8DDE9C5C31AF725D249C8C40FD605680FF30797BF92BC9C7491A415F67523D0428570300B432F6E4
            000D5AE40002747258E8B7FD523D2A909105712F3BF99742BD8DDE9C5C31AF725D249C8C6D7F706B4A6C4BC41BC90EB431323B055F6752420428AC8A2001BA7886
            000D5AE500027FEAC76874A818459243729B712F3BF99742BD8DDE9C5C31AF725D249C8CA1C1D8EBB0F869C488CA4D84D0BBA7B05F675248042961A230062BC62E
            000D5AE60002E7A4D0FD2AEE2609AD68D42D712F3BF99742BD8DDE9C5C31AF725D249C8C79CA67571A357355489925D17FA3DF245F675271042A142D3004F9F870
            000D5AE7000208485549F7F1A169E5F826FE712F3BF99742BD8DDE9C5C31AF725D249C8C28ED961AD86D8434F9224BB02A10B4CB5F6752950429BCD02000E5E32F
            000D5AE80002A3A38BFBDFA87A52A296B70C712F3BF99742BD8DDE9C5C31AF725D249C8C0FA44BE36D28A7679F3FF6DBEBE5EEAE5F6752AE04298EFA20099534A1
            000D5AE90002BD060FEAB035BB87A6129460712F3BF99742BD8DDE9C5C31AF725D249C8C28D2CCA2180645A76FF3D5E58667B95B5F6753010429B5422001BFD6EA
            000D5AEA00020260737A2B02AFFC94FFFE70EAB035BB87A6129460712F3BF99742BD8DDE4338DD580E48F8309B021FAA32D234835F6753070428349B00FCE1B952
            000D5AEB00027E915946F31C66A9CFBA8811EAB035BB87A6129460712F3BF99742BD8DDE1BB0480E31BE5FBBA43CDFCEBB38A7D65F6753110428E07B0000C46A2C
            000D5AEC00027FD005BFBF46169FBD5A6013EAB035BB87A6129460712F3BF99742BD8DDEBBF03409427D17CC8B2FEE1D189095765F67531C04296F4F3000C03CF6
            000D5AED0002A173344649A8A50FB0CA5407EAB035BB87A6129460712F3BF99742BD8DDE0FFEE1303E90F129367DBE0E1139F4C25F6753340429F9740016ABFC07
            000D5AEE0002956AEAECD5D07A0F4095DE6EEAB035BB87A6129460712F3BF99742BD8DDE32B9A5629D4D3ED2FB86FF07561B37205F67535B042A25D320016D70A2
            000D5AEF00027422AA1CBBDEB1B6DBAF9ADCEAB035BB87A6129460712F3BF99742BD8DDEBF6511990EC4ED17FE807C4E9FCEBA0F5F6753760429E0F8300225EEF3
            000D5AF0000244E4DEA23EF4688D1F1CE458EAB035BB87A6129460712F3BF99742BD8DDEE24E94E7EB17AE8D63E5809FB403AD815F67537D0429F8FF300292435A
            000D5AF10002AE7AB1E0A7A45250759E90CEEAB035BB87A6129460712F3BF99742BD8DDEC07B6C20A52215DD75C4EA9F5B82EDA45F6753DE042AA4D900619AEFC0
            000D5AF20002E65749E8AAC3FF2EF19230A5EAB035BB87A6129460712F3BF99742BD8DDE7A87159CA0976404D52AB06915F3B4E85F67542F0428B57D3003BC90CD
            000D5AF300026F05B95E2CE69904B25EEAD9EAB035BB87A6129460712F3BF99742BD8DDEB29BBF6FE027464E5055BC3136F9F4485F67545104275B072000D163BB
            000D5AF400027BF0B5A3C9DC48F81019D0F7EAB035BB87A6129460712F3BF99742BD8DDE4247AC5F45E5A3DC5A062DC58FC76B485F67548C042745243001C6F8DB
            000D5AF50002DBF56321958600CA3BD4F74AEAB035BB87A6129460712F3BF99742BD8DDEA969D32521B6FEAF9308B3471BED9B295F67549204268C62009882DF08
            000D5AF60002D40B5147CBA9473229D3DF83EAB035BB87A6129460712F3BF99742BD8DDE1951D2E13127CC5438ACF1D354E52FCF5F6754C80427268E00AF61A507
            000D5AF7000235A2B85243DBC5C24BFBBBDFEAB035BB87A6129460712F3BF99742BD8DDE5650722295DD8835B4A4848B405AD0A65F67550704268C012001E57454
            000D5AF80002BC004F1B77A11EC2C3BF7025EAB035BB87A6129460712F3BF99742BD8DDEC4C2F07701BC421600108911357EEBEC5F6755430425C3803004DFCAB5
            000D5AF90002230EAA589798E1D8E07706DCEAB035BB87A6129460712F3BF99742BD8DDE1C1C50D52EC2B738B84A20E0050147545F675552042515D73009012A53
            000D5AFA00028B0965E826D244A221171C8EEAB035BB87A6129460712F3BF99742BD8DDE32F75321091F328FF22491733BD20DDD5F675582042570D0200444D7C9
            000D5AFB000275E5D0C0A87ECA821CD83FC9EAB035BB87A6129460712F3BF99742BD8DDEA607A8E6F393162ABBFD2B557BFD71C55F67558E042508051003F8E2FF
            000D5AFC000270DCEBC4783C86FC1C434FD6EAB035BB87A6129460712F3BF99742BD8DDE42FB985BDBEEF359517B37E5D8C814C65F6755A4042574892003E9D9F4
            000D5AFD0002B83375A1514C7A055A2E6568EAB035BB87A6129460712F3BF99742BD8DDEAEA1D1F50206C8BF05E4849028C6F8A25F6755B20425A440100A8A840D
            000D5AFE000231772523D5B8923C64D2A359A1514C7A055A2E6568EAB035BB87A612946047DD4C95D6E6C3560190A89CDB2287765F6755E5042605753001D0E776
            000D5AFF0002A205DCDD18F7BA86EBCE0AF6A1514C7A055A2E6568EAB035BB87A6129460E4616D6B8ACD4545BB8E54909DD0DD7A5F6755EC0425880600053E639D
            000D5B0000027F98F36218C2AE6C85C74768A1514C7A055A2E6568EAB035BB87A61294606E6F24AB856B05927816C480F016D1835F6755F8042615B83004D4B4B4
            000D5B010002DFE309B13FEE3FB1C8EEFCF3A1514C7A055A2E6568EAB035BB87A61294606243E3C5401A1551EE0393BC0753A6755F675640042684321002624EBF
            000D5B02000274420FDBA2AD659E59328D1EA1514C7A055A2E6568EAB035BB87A612946058365293427478A4F1D8AB3D4575368F5F67567F0425814E2002BA1ED1
            000D5B030002537822F67FA4875678459CA2A1514C7A055A2E6568EAB035BB87A6129460AFB762B9E60AA75F028497ED5C3871165F6756880424C4721001BCAF7A
            000D5B0400029863B8E8461E8A6C22039663A1514C7A055A2E6568EAB035BB87A6129460A66D8FF4FC90F63211FB1FAD32EA1E1A5F6756A50425418A0068EDB065
            000D5B0500020E33F330CAD58A77D4E52AC5A1514C7A055A2E6568EAB035BB87A6129460B57A8FA97C47410BDCDCC6721F280C635F6757160425478F2004A0EAB2
            000D5B06000217F7736E5A96733293079B1FA1514C7A055A2E6568EAB035BB87A6129460C2759C615E02E9D42A0F1881824500F05F675725042375A620015F6A42
            000D5B0700028A40D6D5DEEDE6EDAC2AC75BA1514C7A055A2E6568EAB035BB87A6129460D1B280687694EEF04F3FEA81C8CB0B5F5F67572B0423CBF20074A3CA91
            000D5B0800027F3210755FA7A42F2BF3BA4BA1514C7A055A2E6568EAB035BB87A6129460A6DE1F4D16A9547F1B250FA28703209D5F675738042451542006394C81
            000D5B090002DC33D6023230B2C03F81F434A1514C7A055A2E6568EAB035BB87A6129460EA9C45A62596C910813EE274464DB8B65F6757620424AE8B10045A6C41
            000D5B0A0002B1E881D6B25592572A5D4F17A1514C7A055A2E6568EAB035BB87A612946016BB592DDC2EEC63E417876D0C85DEAE5F6757A5042468BC1000E7F060
            000D5B0B0002E2882B4F37790CE363564CE2A1514C7A055A2E6568EAB035BB87A6129460E6A291459A73E382CC6E765FC13DCFE45F6757A904239D171004DB21DF
            000D5B0C0002C060A678338119B99C003DDFA1514C7A055A2E6568EAB035BB87A61294603169E2D1E6C092AE712521177A9C69B75F6757B704242A41200489B471
            000D5B0D00021A97B16ABB408CD516AD1F78A1514C7A055A2E6568EAB035BB87A61294601700305CBE3057021AE003955EEF94455F67585F042484FF20021E4F21
            000D5B0E00029ED0C1A6D59FA31B41D877EAA1514C7A055A2E6568EAB035BB87A612946026246AE1B57E4EFE5B613D6891CDB8295F6758660421A54A00E1693B68
            000D5B0F0002A1B9E579769AB45BF09BAFE2A1514C7A055A2E6568EAB035BB87A612946028B5D65B9A5801A15155BBEA84EE7B625F67586D04221F6720060F56B7
            000D5B10000276B1B7C6C903434D82DA2D2AA1514C7A055A2E6568EAB035BB87A61294606ECFC192E5C151B3A8C63BE6F9CC20A55F6758940422950300F90FDF7C
            000D5B11000271F44D2E16B30A871B1F981CA1514C7A055A2E6568EAB035BB87A6129460CD600DA86A8A657C304CEA0A071B7C255F6758AF0422663C200B171654
            000D5B120002F8CCE1F9D756E4D098B7E3722E16B30A871B1F981CA1514C7A055A2E65689B3DB6F25FE16E5395C6AF8260011FA95F6759060422753D100405333D
            000D5B130002A57BA5E1B512EE1A75971E8A2E16B30A871B1F981CA1514C7A055A2E6568A35A4099F519A3681995A39D2BCA48BE5F6759120421582A2005E450B0
            000D5B140002A4C0387D4E6711A1073821372E16B30A871B1F981CA1514C7A055A2E6568EC4E97E433358E689630696F3A77F5845F6759380421B41A2005377F75
            000D5B15000293297BF7CBB927F41868C9272E16B30A871B1F981CA1514C7A055A2E656894D2AD441766049634674688F4DBEA265F67597F04218DCA1006AB0CAD
            000D5B1600028CA092D0C3997BD51521F4DF2E16B30A871B1F981CA1514C7A055A2E656844F48B4658EA780B5D05A9EA15A9B5A45F6759A80420CBF310004CDBD7
            000D5B170002D72FB62FDC56280565F4EF262E16B30A871B1F981CA1514C7A055A2E656854D2BB7F258C628DF407843E39D576D65F6759BE04209AFA0063C638AB
            000D5B180002D9B9658E51132B3F008ABE822E16B30A871B1F981CA1514C7A055A2E6568A922EF15048A84BF1EF63118A90A38755F6759D10420C0AC006845FAD2
            000D5B190002F9A3CE4C133EB394418629CB2E16B30A871B1F981CA1514C7A055A2E65682200F9F624ED5C164ECDC414322F63A75F6759DD0420F5BF008B5DCBE8
            000D5B1A00028DBCF80E438E8850C31B72F02E16B30A871B1F981CA1514C7A055A2E656828A3121EC29E46922E6B1DE0E2FD5D605F675A3C04214C600091B9A177
            000D5B1B00027D7367179CF282AB6E56DA0E2E16B30A871B1F981CA1514C7A055A2E6568BCF8FB6E95E3FC1B8B86072E82266E835F675A4E04201B451000B36309
            000D5B1C0002FFE6925E2FBCF387D579DE2A2E16B30A871B1F981CA1514C7A055A2E6568A54416D2D5D9493C1DC84DFCB20E7FFE5F675A540420523910047F2D25
            000D5B1D0002CC09AA4175A2A0910C86DF2E2E16B30A871B1F981CA1514C7A055A2E6568B23C79446E0C8753AEDDAEF95D2E62F85F675A610420C2AA00A68608BE
            000D5B1E000263149F0C286725F93D9A654B2E16B30A871B1F981CA1514C7A055A2E65688CEA6187200080503599FE05D8A183745F675A85042113B0100097ED26
            000D5B1F0002A834BDFF25238FB71B201C232E16B30A871B1F981CA1514C7A055A2E65680718F0A8FFC29C2FAFE07E9C7CCE48355F675A880420F4152001824ABB
            000D5B2000020DA0F8966B2C3BA00D0E5EAB2E16B30A871B1F981CA1514C7A055A2E656888A5DF385771CBF99D0D4FE4038A6EDC5F675AA3042178481004D5CE34
            000D5B210002E29DE11E30C216CB818AAA682E16B30A871B1F981CA1514C7A055A2E6568BBD305DA13D6806D574035615ACC455E5F675AB1042187391000BB7D47
            000D5B2200024BAA577D843D2868485263142E16B30A871B1F981CA1514C7A055A2E65684E18A1F1D13DA70257B9DBBB4E61C5B25F675ABD0421D32310009DD446
            000D5B230002BB838C2ECFC09EF7C81960622E16B30A871B1F981CA1514C7A055A2E6568E6067F649DD835A4A0810F31F545E74C5F675ABF04222EB410078FF097
            000D5B240002F4D82A6FA12D9FB53B0C96F22E16B30A871B1F981CA1514C7A055A2E656851D3358D51773C498B5D61E21DAD7AFE5F675AEB0422C46D00D434FDF9
            000D5B25000258F5D60BE9D660C9C516ABD12E16B30A871B1F981CA1514C7A055A2E65686357F4537F59E4F4202803EA3A02E1EF5F675B000422797A2002835543
            000D5B2600026A7884439C46DB6A678238900BE9D660C9C516ABD12E16B30A871B1F981C69B6A013B180BF41EA8F61FE0C8A4E085F675B080422AA4830069EEC34
            000D5B270002F06372E1955168C412B0D7980BE9D660C9C516ABD12E16B30A871B1F981C8B0754A2B25A2C077873BF22E15A7F165F675B2F042323A82005926BB2
            000D5B280002679C3B6D5D001DB3D141BB4D0BE9D660C9C516ABD12E16B30A871B1F981C06F04A5FAD47BC51578AC39D664C1F065F675B540422F13220084323FF
            000D5B2900021B3C46BA98108F4B93BEF5630BE9D660C9C516ABD12E16B30A871B1F981C5444104ED8F2D9B76E13D0B25687A4835F675B830422CC7B20090DAFFA
            000D5B2A000250A38652D0FAFB0FC9E4A9850BE9D660C9C516ABD12E16B30A871B1F981C315EC036F2FB56B1299DFB79404262955F675BB3042271E40015CE5433
            000D5B2B0002F43957F7C45F41376F06DAE60BE9D660C9C516ABD12E16B30A871B1F981C1D2A05C83F2708B5D9681599E6EB24105F675BB50422148F2001AD547F
            000D5B2C0002CCAE4B5EC453D5ED5677A5710BE9D660C9C516ABD12E16B30A871B1F981C48FBDE656DA6DD2859159D92470E565A5F675BCF0422A45E00201C8383
            000D5B2D0002F7067B2F4B823E7871E291220BE9D660C9C516ABD12E16B30A871B1F981C280F1F20AA0C8B463FDDF489AD5808305F675BD10422BB04002FBA7169
            000D5B2E00020CCCF37775D780CB6C4AB5490BE9D660C9C516ABD12E16B30A871B1F981CE86545E961E0D98A7BD28083971F2C845F675BFC0423579D200178658E
            000D5B2F0002D0A9005C0CF735A13525523C0BE9D660C9C516ABD12E16B30A871B1F981C877647CF955CED2A95D8D20AF5649D515F675C1804230D123005D4609D
            000D5B300002421B7352E4C670BE6D13AC950BE9D660C9C516ABD12E16B30A871B1F981C070181BC77D98D11FEFBDC81320540775F675C400423195820024C28EA
            000D5B3100026A7760344D91427439581ECE0BE9D660C9C516ABD12E16B30A871B1F981C7201340F978293EF9BFACB90B2E3226B5F675C480422DF503002902E82
            000D5B32000284553C2AC155724BD871B3E00BE9D660C9C516ABD12E16B30A871B1F981CC227681ECACB6EE81C6E516014F66D0B5F675C5004235D55004DBFA3B0
            000D5B3300028B2E15AEAB83929FE0321C8F0BE9D660C9C516ABD12E16B30A871B1F981C18A972BF24774C4293740829CC9D70CE5F675C520423DA4130017C2378
            000D5B3400021B743FA2AE78F9FE98E51C6D0BE9D660C9C516ABD12E16B30A871B1F981C5CFC434A24F4A0981A8A9AF20C525AE65F675C620424833220013B4298
            000D5B350002064106CD3DBA9C8CF2B18DDF0BE9D660C9C516ABD12E16B30A871B1F981C3225B4E69629B933C58CDD21FD4AB4A65F675C7D0424D97A0068DA299E
            000D5B360002ED753DB4D091E8B88BE625D20BE9D660C9C516ABD12E16B30A871B1F981CD40CD629590424EEBE42501B9B6A8CB45F675C9F0424ED232005B1A751
            000D5B3700023B868D83DA476D2252C483E50BE9D660C9C516ABD12E16B30A871B1F981C9A6798736453C014C31E418CE13F2FD95F675CB10424D61A10043F8492
            000D5B380002D3395B9C2AC189D2616580D60BE9D660C9C516ABD12E16B30A871B1F981C4A8D1C3601DBB047D1E29F8ECD0873625F675CD5042523A62001CA9BF8
            000D5B390002ADCAE5BAB1227729B3536E830BE9D660C9C516ABD12E16B30A871B1F981CCA4E4D9B2D8BFE27D994AB9FBA90F1975F675CEF0424FF50008EAF803B
        """.trimIndent()
        Context.create(defaultTestNetParameters)
        Security.addProvider(BouncyCastleProvider())
        val blocks = blocksString.split("\n").map { SerializeDeserializeService.parseVeriBlockBlock(it.asHexBytes()) }
        for (block in blocks) {
            val time = measureTimeMillis {
                block.hash
            }
            println("Hashing block ${block.hash} @ ${block.height} took ${time}ms")
        }
    }
}
