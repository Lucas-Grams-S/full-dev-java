package com.payflow.lab.grpc;

import static org.assertj.core.api.Assertions.assertThat;

import com.payflow.lab.grpc.evolution.ReplyV1;
import com.payflow.lab.grpc.evolution.ReplyV2Broken;
import com.payflow.lab.grpc.evolution.ReplyV2Compatible;
import com.payflow.lab.grpc.antifraud.v1.Decision;
import org.junit.jupiter.api.Test;

class ProtoEvolutionTest {

    @Test
    void newFieldWithNewNumberIsCompatibleBothWays() throws Exception {
        byte[] v1 = ReplyV1.newBuilder().setId("a").setScore(42).build().toByteArray();
        ReplyV2Compatible newReader = ReplyV2Compatible.parseFrom(v1);   // leitor NOVO, dado ANTIGO
        byte[] v2 = ReplyV2Compatible.newBuilder().setId("a").setScore(42).setModelVersion("m7").build().toByteArray();
        ReplyV1 oldReader = ReplyV1.parseFrom(v2);                       // leitor ANTIGO, dado NOVO
        System.out.println("T4G campo novo: leitor novo lendo dado antigo -> modelVersion='" + newReader.getModelVersion() + "' (padrao); leitor antigo lendo dado novo -> score="
                + oldReader.getScore() + ", campo desconhecido preservado=" + oldReader.getUnknownFields().asMap().keySet());
        assertThat(oldReader.getScore()).isEqualTo(42);
        assertThat(newReader.getModelVersion()).isEmpty();
    }

    @Test
    void reusingAFieldNumberWithAnotherTypeLosesDataSilently() throws Exception {
        byte[] v1 = ReplyV1.newBuilder().setId("a").setScore(42).build().toByteArray();
        ReplyV2Broken reader = ReplyV2Broken.parseFrom(v1);              // NÃO lança exceção
        System.out.println("T4G numero reutilizado (int32 -> string): parse OK, id='" + reader.getId() + "', reason='" + reader.getReason()
                + "', campo 2 virou 'desconhecido'=" + reader.getUnknownFields().hasField(2) + " (o score 42 sumiu sem erro)");
        assertThat(reader.getReason()).isEmpty();
        assertThat(reader.getUnknownFields().hasField(2)).isTrue();
    }

    @Test
    void enumValueUnknownToOldClientReadsAsUnrecognized() throws Exception {
        // um servidor NOVO envia o valor 7 (inexistente neste cliente): o cliente antigo vê UNRECOGNIZED, não uma exceção
        var bytes = com.payflow.lab.grpc.antifraud.v1.ScoreResponse.newBuilder().setScore(10).setDecisionValue(7).build().toByteArray();
        var r = com.payflow.lab.grpc.antifraud.v1.ScoreResponse.parseFrom(bytes);
        System.out.println("T4G enum com valor novo (7) lido por cliente antigo: decision=" + r.getDecision() + ", valor numerico preservado=" + r.getDecisionValue());
        assertThat(r.getDecision()).isEqualTo(Decision.UNRECOGNIZED);
    }
}
