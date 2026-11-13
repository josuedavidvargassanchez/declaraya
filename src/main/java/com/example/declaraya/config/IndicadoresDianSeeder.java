package com.example.declaraya.config;

import com.example.declaraya.models.IndicadorDian;
import com.example.declaraya.repositories.IndicadorDianRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

// Cifras publicas de la DIAN (recaudo, declarantes y UVT) para el dashboard de Power BI.
// Son datos agregados que la DIAN publica; la DIAN no publica datos de contribuyentes individuales.
@Component
public class IndicadoresDianSeeder implements CommandLineRunner {

    @Autowired
    private IndicadorDianRepository indicadorRepository;

    @Override
    public void run(String... args) {
        if (indicadorRepository.count() > 0) {
            return;
        }
        indicadorRepository.saveAll(List.of(
            new IndicadorDian(2025, "Año completo", "Recaudo bruto", "Recaudo bruto total", 296000000L, "Millones COP", "DIAN (vía El Espectador / Portafolio)", "296 billones de pesos"),
            new IndicadorDian(2025, "Año completo", "Recaudo bruto", "Retención en la fuente de renta", 103600000L, "Millones COP", "DIAN (vía El Espectador / Portafolio)", "35 % del recaudo anual"),
            new IndicadorDian(2025, "Año completo", "Recaudo bruto", "IVA", 64300000L, "Millones COP", "DIAN (vía El Espectador / Portafolio)", "21,7 % del recaudo anual"),
            new IndicadorDian(2025, "Año completo", "Recaudo bruto", "Tributos de aduanas", 50460000L, "Millones COP", "DIAN (vía El Espectador / Portafolio)", "17,1 % del recaudo anual"),
            new IndicadorDian(2025, "Año completo", "Recaudo bruto", "Tributos de actividad económica interna", 245530000L, "Millones COP", "DIAN (vía Portafolio)", "82,95 % del recaudo anual"),
            new IndicadorDian(2025, "Ene-Jun", "Recaudo bruto", "Retención en la fuente", 48200000L, "Millones COP", "DIAN (vía Radio Nacional)", "32,3 % del recaudo del semestre"),
            new IndicadorDian(2025, "Ene-Jun", "Recaudo bruto", "IVA", 31700000L, "Millones COP", "DIAN (vía Radio Nacional)", "21,3 % del recaudo del semestre"),
            new IndicadorDian(2025, "Ene-Jun", "Recaudo bruto", "Tributos aduaneros", 24300000L, "Millones COP", "DIAN (vía Radio Nacional)", "16,3 % del recaudo del semestre"),
            new IndicadorDian(2025, "Ene-Jun", "Recaudo bruto", "Impuesto de renta", 23700000L, "Millones COP", "DIAN (vía Radio Nacional)", "15,8 % del recaudo del semestre"),
            new IndicadorDian(2025, "Ene-Jun", "Recaudo bruto", "Otros impuestos", 21300000L, "Millones COP", "DIAN (vía Radio Nacional)", "14,3 % del recaudo del semestre"),
            new IndicadorDian(2022, "Año completo", "Recaudo bruto", "Recaudo bruto total", 229000000L, "Millones COP", "DIAN (vía El Tiempo)", "229 billones de pesos"),
            new IndicadorDian(2022, "Año completo", "Recaudo neto", "Recaudo neto total", 211000000L, "Millones COP", "DIAN (vía El Tiempo)", "211 billones de pesos"),
            new IndicadorDian(2025, "Año completo", "Recaudo neto", "Recaudo neto total", 270000000L, "Millones COP", "DIAN (vía El Tiempo)", "270 billones de pesos"),
            new IndicadorDian(2025, "Ene-May", "Recaudo bruto", "Recaudo bruto acumulado", 127300000L, "Millones COP", "DIAN (vía Portafolio / ANIF)", "Base de comparación de 2026"),
            new IndicadorDian(2026, "Ene-May", "Recaudo bruto", "Recaudo bruto acumulado", 139300000L, "Millones COP", "DIAN (vía Portafolio / ANIF)", "Crecimiento nominal de 9,4 % frente a 2025"),
            new IndicadorDian(2021, "Año completo", "Declarantes", "Declarantes de renta", 4500000L, "Personas", "DIAN (vía El Tiempo / GDA)", "Base de declarantes del impuesto de renta"),
            new IndicadorDian(2025, "Año completo", "Declarantes", "Declarantes de renta", 7200000L, "Personas", "DIAN (vía El Tiempo / GDA)", "Crecimiento de 60 % frente a 2021"),
            new IndicadorDian(2025, "Año gravable", "Declarantes", "Personas naturales obligadas a declarar renta (hasta)", 7000000L, "Personas", "DIAN (vía Valora Analitik)", "Declaración presentada en 2026"),
            new IndicadorDian(2025, "Año gravable", "Recaudo esperado", "Recaudo estimado renta personas naturales", 6100000L, "Millones COP", "DIAN (vía Valora Analitik)", "6,1 billones de pesos"),
            new IndicadorDian(2023, "Año gravable", "UVT", "Valor de la UVT", 42412L, "COP", "DIAN, Resolución 001264 de 2022", ""),
            new IndicadorDian(2024, "Año gravable", "UVT", "Valor de la UVT", 47065L, "COP", "DIAN, Resolución 000187 de 2023", ""),
            new IndicadorDian(2025, "Año gravable", "UVT", "Valor de la UVT", 49799L, "COP", "DIAN, Resolución 000193 de 2024", ""),
            new IndicadorDian(2026, "Año gravable", "UVT", "Valor de la UVT", 52374L, "COP", "DIAN, Resolución 000238 de 2025", "Fijado el 15 de diciembre de 2025")
        ));
        System.out.println("Indicadores publicos de la DIAN cargados: " + indicadorRepository.count());
    }
}
