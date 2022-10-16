package substratum.theme.template

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

/**
 * SettingsActivity é uma Atividade
 * Atividades são telas contendo algum conteudo
 */
class SettingsActivity : AppCompatActivity() {

    /**
     * Ao inicializar o app, iremos construir um Shell
     * esse Shell sera utilizado para executar comandos como root
     * Alem disso, estamos utilizando uma biblioteca chamada de libsu
     * para executar os comandos no shell (biblioteca do criador do Magisk)
     * >> https://github.com/topjohnwu/libsu
     */
    companion object {
        init {
            // Caso a build do app for "DEBUG", habilitar verboseLogging no Shell
            Shell.enableVerboseLogging = BuildConfig.DEBUG

            // Criação do shell
            Shell.setDefaultBuilder(
                Shell.Builder.create()
                    .setFlags(Shell.FLAG_REDIRECT_STDERR)
                    .setTimeout(10)
            )
        }
    }

    /**
     * onCreate: Quando o aplicativo for criado
     * Activity lifecycle: https://developer.android.com/guide/components/activities/activity-lifecycle
     *
     * Sempre que nossa atividade for construida, esse método onCreate vai ser chamado
     * Ele pode ser chamado quando:
     * - Quando o app estava fechado, e o usuário abriu ele
     *    afinal, a atividade contendo nosso app tem que ser criada pro usuário utilizar, não é mesmo?
     * - Quando a atividade teve de ser recriada
     *    como, quando o celular trocou do modo retrato pro paisagem ou vice-versa
     *    como, quando o tema do celular trocou (tema escuro -> tema claro ou vice-versa)
     *  E outros casos...
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        Shell.getShell {}
        super.onCreate(savedInstanceState)

        // Define o "settings_activity" como layout dessa atividade
        setContentView(R.layout.settings_activity)

        /**
         * savedInstanceState sempre é nulo quando o app está sendo criado pela primeira vez:
         *  - Tipo quando o app está fechado, e o usuário abre o app.
         *
         * savedInstanceState sempre é diferente de nulo, quando existe uma instancia da atividade salva:
         *  - Tipo quando o tema do celular trocou, ou o celular teve a orientação trocada.
         */
        if (savedInstanceState == null) {

            /**
             * Cria um fragmento, contendo as configurações do app.
             * Conteudo das configurações na nossa atividade, é declarado em: R.id.settings
             * Programação desse fragmento, é posto em: SettingsFragment()
             */
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        /**
         * Quando o usuário abrir o app, ele precisa dar permissão de root pra executar as coisas
         * Porem, não sabemos se o usuário permitiou ou rejeitou a solicitação.
         *
         * Shell.isAppGrantedRoot() retorna "null" quando o usuário não executou nenhuma ação no popup de root
         * Shell.isAppGrantedRoot() retorna "true" quando o usuário aceitou a permissão de root
         * Shell.isAppGrantedRoot() retorna "false" quando o usuário rejeitou a permissão de root (ou não possui root)
         *
         * Aqui em baixo, a gente cria um novo Thread (utilizando o Dispatchers.IO)
         * E enquanto "Shell.isAppGrantedRoot()" for igual a "null", a gente espera 0.1 segundo,
         * e as coisas ficam nesse loop, até "Shell.isAppGrantedRoot()" retorne algo diferente de nulo.
         *
         * Quando "Shell.isAppGrantedRoot()" retornar algo diferente de nulo, a execução do código segue em frente..
         */
        lifecycleScope.launch(Dispatchers.IO) {
            while (Shell.isAppGrantedRoot() == null) {
                delay(100)
            }
            /**
             * Se "Shell.isAppGrantedRoot()" for true, significa que foi concedido acesso root ao nosso app
             * Quando isso acontece, não queremos fazer mais nada, afinal, o app já pode funcionar corretamente.
             *
             * Porem, quando "Shell.isAppGrantedRoot()" for false, significa que não temos root (ou não foi nos concedido)
             * Pra inverter resultado, usamos "!":
             *
             * Imagine com Shell script:
             *
             * Se $teste for verdadeiro, execute algo
             *  if [[ $teste == true ]]; then...
             *
             * Se $teste for diferente de true (ou seja, falso), execute algo
             *  if [[ $teste == !true ]]; then...
             *
             * Obviamente !true não funciona em shell script, porem, em Kotlin sim kk
             *
             * Ou seja:
             *   - se "Shell.isAppGrantedRoot()" retorna verdadeiro, essa condição é verdadeira.
             *   - se "!Shell.isAppGrantedRoot()" retorna verdadeiro, essa condição é invertida por conta do !
             *       o que faz o do resultado dela "falso".
             *
             *  Se "Shell.isAppGrantedRoot()" retorna verdadeiro, o root foi permitido
             *  Porem, se "!Shell.isAppGrantedRoot()" retornar verdadeiro, o root não foi permitido.
             *
             *  Colocamos o !! no final do "Shell.isAppGrantedRoot()!!"
             *    porque isAppGrantedRoot() é um método que pode retornar "null"
             *    e kotlin é uma linguagem null safety.
             *
             * Se "!Shell.isAppGrantedRoot()!!" for verdadeiro, iremos chamar o "showNonRootDialog()"
             * Chamamos ele no Thread da UI, pq é um dialogo que vai ser desenhado na UI,
             * e anteriormente criamos um Thread com o Dispatchers.IO.
             *
             */
            if (!Shell.isAppGrantedRoot()!!) {
                runOnUiThread { showNonRootDialog(this@SettingsActivity) }
            }
        }

    }

    /**
     * Uma função que cria um dialogo.
     *
     * funções começam com "fun", de função mesmo.
     *
     *    Em shell script, uma função é mais ou menos assim:
     *
     *    function <nomeDaFuncao>(){
     *      echo $1 # mostra na tela o argumento 1 especificado na execução da função.
     *      return 0 # retorno, não é obrigatorio, mas funções podem retornar coisas.
     *    }
     *
     *    Já em kotlin, temos a mesma coisa, só que mais ou menos assim:
     *
     *    fun <nomeDaFunção> (<Argumentos>): <Retorno, caso necessário> {
     *       // Operação da função
     *    }
     *
     *    =================
     *
     *    Mostrar na tela "Olá, $argumento" em shell:
     *
     *    #!/bin/sh
     *    function oi(){
     *      echo "Olá, $1"
     *    }
     *
     *    oi "Sapo" # chama a função "oi"
     *
     *    O mesmo em Kotlin:
     *
     *    // String = um texto
     *
     *    fun oi(argumentoUm: String){
     *       println("Olá, $argumentoUm")
     *    }
     *
     *    oi("Sapo") // chama a função "oi"
     *
     *    ================
     *
     *    Verificar se o número é par:
     *
     *    function e_par(){
     *      if [[ $(expr $1 % 2) == 0 ]]; then
     *            return true
     *         else;
     *            return false
     *         fi
     *    }
     *
     *    # verifica se 4 é par
     *    resultado = $(e_par 4)
     *    echo Resultado: $resultado
     *
     *    O mesmo em kotlin:
     *
     *    // Int = Número inteiro
     *    // Boolean = Algo que pode ser "verdadeiro ou falso"
     *
     *    fun ePar(valor: Int): Boolean {
     *      if (valor % 2 == 0) {
     *         return true
     *      } else {
     *         return false
     *      }
     *
     *    val resultado = ePar(4)
     *    println("Resultado: $resultado")
     *
     *    A função abaixo, chama o "MaterialAlertDialogBuilder"
     *    e cria um novo dialogo no Contexto especificado.
     *
     */
    fun showNonRootDialog(context: Context) {
        val alertDialogBuilder = MaterialAlertDialogBuilder(context)
        alertDialogBuilder.setTitle(R.string.error) /* Título do dialogo */
        alertDialogBuilder.setMessage(R.string.error_msg) /* Texto dele */
        alertDialogBuilder.setCancelable(false)

        /* Ação ao clickar no botão "Positivo" */
        alertDialogBuilder.setPositiveButton(
            getString(android.R.string.ok)
        ) { _, _ ->
            // nesse caso, só encerra todas as atividades do nosso app
            finishAffinity()
        }

        val alertDialog: AlertDialog = alertDialogBuilder.create()
        alertDialog.show() // mostra o dialogo na tela
    }

}