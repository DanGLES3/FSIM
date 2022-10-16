package substratum.theme.template

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.topjohnwu.superuser.Shell

/**
 * SettingsFragment é um fragmento que guarda as configurações.
 */
class SettingsFragment : PreferenceFragmentCompat() {

    /**
     * Quando as preferencias forem criadas (similar ao onCreate, só que da tela de configurações)
     */
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        /**
         * Usar o: R.xml.root_preferences como as configurações dessa tela
         * Aperte ctrl e click no "root_preferences" para mais detalhes dessa tela.
         */
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        /**
         * Registra a preferencia nomeada de "gesture_options", ela está declarada em "root_preferences"
         */
        val gesturePref = findPreference("gesture_options") as Preference?

        /**
         * Quando "gesturePref" (preferencia "gesture_options") for alterada
         * o código a baixo será executado
         */
        gesturePref!!.setOnPreferenceChangeListener { preference, newValue ->
            // selectedValue == nome da preferencia que foi alterada
            val selectedValue = newValue as String

            // caso o nome da preferencia for:
            when (selectedValue) {
                // se for "disabled_completely", executar o código a baixo
                "disabled_completely" -> {
                    writeSecureSetting("back_gesture_inset_scale_left", "-1")
                    writeSecureSetting("back_gesture_inset_scale_right", "-1")
                }

                // se for "left_only", executar o código a baixo
                "left_only" -> {
                    writeSecureSetting("back_gesture_inset_scale_left", "-1")
                    deleteSecureSetting("back_gesture_inset_scale_right")
                }

                // se for "enabled", executar o código a baixo
                "enabled" -> {
                    deleteSecureSetting("back_gesture_inset_scale_left")
                    deleteSecureSetting("back_gesture_inset_scale_right")
                }
            }
            return@setOnPreferenceChangeListener true
        }

    }

    /**
     * Método que simplifica a escrita em Secure Setting
     */
    private fun writeSecureSetting(prop: String, value: String) {
        val cmd = "settings put secure $prop $value"
        Shell.cmd(cmd).submit()
    }

    /**
     * Método que simplifica deletar uma Secure Setting
     */
    private fun deleteSecureSetting(prop: String) {
        val cmd = "settings delete secure $prop"
        Shell.cmd(cmd).submit()
    }

    /**
     * Método de exemplo que abre um link
     */
    private fun abrirUrl(url: String) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(browserIntent)
    }

    /**
     * Método de exemplo que cria um dialogo
     */
    private fun mostrarDialogo(
        // o simbolo de =, cria um valor padrão pro argumento
        // caso o argumento não for especificado, o padrão é utilizado.

        titulo: String = "",
        texto: String = "",
        textoAcaoUm: String = "",
        textoAcaoDois: String = "",
        acaoUm: () -> Unit = {},
        acaoDois: () -> Unit = {},
    ) {
        val alertDialogBuilder = MaterialAlertDialogBuilder(requireContext())
        alertDialogBuilder.setTitle(titulo)
        alertDialogBuilder.setMessage(texto)
        alertDialogBuilder.setCancelable(true)

        if (textoAcaoUm.isNotEmpty())
            alertDialogBuilder.setPositiveButton(
                textoAcaoUm
            ) { _, _ ->
                acaoUm()
            }

        if (textoAcaoDois.isNotEmpty())
            alertDialogBuilder.setNegativeButton(
                textoAcaoDois
            ) { _, _ ->
                acaoDois()
            }

        val alertDialog: AlertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }
}