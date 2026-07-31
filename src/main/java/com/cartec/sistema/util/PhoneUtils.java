package com.cartec.sistema.util;

/**
 * Padronizacao de numero de telefone para disparos de CRM/promocionais
 * (ver CLAUDE.md, objetivo 2): garante DDI 55 na frente e so digitos,
 * no formato exigido pelo link wa.me (ex: 5584999998888).
 */
public final class PhoneUtils {

    private PhoneUtils() {
    }

    public static String padronizar(String bruto) {
        if (bruto == null) {
            return null;
        }
        String digitos = bruto.replaceAll("\\D", "");
        if (digitos.isEmpty()) {
            return "";
        }
        if (digitos.length() > 11 && digitos.startsWith("0")) {
            digitos = digitos.replaceFirst("^0+", "");
        }
        if (digitos.length() == 10 || digitos.length() == 11) {
            digitos = "55" + digitos;
        }
        return digitos;
    }

    public static boolean isValido(String telefonePadronizado) {
        return telefonePadronizado != null
                && telefonePadronizado.startsWith("55")
                && (telefonePadronizado.length() == 12 || telefonePadronizado.length() == 13);
    }
}
