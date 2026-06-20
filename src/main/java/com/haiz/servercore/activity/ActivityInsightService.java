package com.haiz.servercore.activity;

import java.util.ArrayList;
import java.util.List;

public final class ActivityInsightService {
    public List<String> generate(ActivityPeriodSummary current, ActivityPeriodSummary previous,
                                 List<ActivityTopEntry> topPlayers, double retention) {
        List<String> insights = new ArrayList<>();
        if (current.busiestHour() >= 0) {
            int end = (current.busiestHour() + 1) % 24;
            insights.add("O maior movimento ocorreu entre " + hour(current.busiestHour()) + " e " + hour(end)
                    + ". Esse e um bom horario para eventos.");
        }
        double change = previous.totalPlaytimeSeconds() == 0
                ? 0
                : (current.totalPlaytimeSeconds() - previous.totalPlaytimeSeconds()) * 100.0 / previous.totalPlaytimeSeconds();
        if (change <= -15) {
            insights.add("A atividade caiu " + Math.round(Math.abs(change))
                    + "% no periodo. Uma enquete ou evento pode ajudar a reengajar jogadores.");
        } else if (change >= 15) {
            insights.add("A atividade cresceu " + Math.round(change) + "% no periodo analisado.");
        }
        if (current.newPlayers() >= 3 && retention < 35) {
            insights.add(current.newPlayers() + " jogadores novos entraram, mas apenas "
                    + Math.round(retention) + "% jogaram por pelo menos 10 minutos. Vale revisar spawn e tutorial.");
        }
        if (!topPlayers.isEmpty()) {
            insights.add(topPlayers.get(0).name() + " foi o jogador mais ativo e pode receber um reconhecimento.");
        }
        if (insights.isEmpty()) {
            insights.add("A atividade permaneceu estavel, sem variacoes relevantes no periodo.");
        }
        return insights;
    }

    private String hour(int hour) {
        return String.format("%02dh", hour);
    }
}
