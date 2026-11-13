package com.ferramentafacil.app.repository;

import androidx.annotation.Nullable;

import com.ferramentafacil.app.model.Notificacao;
import com.ferramentafacil.app.model.Periodo;
import com.ferramentafacil.app.model.Rental;
import com.ferramentafacil.app.model.RentalStatus;
import com.ferramentafacil.app.model.Tool;
import com.ferramentafacil.app.model.User;
import com.ferramentafacil.app.utils.Constants;
import com.ferramentafacil.app.utils.DateUtils;
import com.ferramentafacil.app.utils.FirebaseErrorUtils;
import com.ferramentafacil.app.utils.MoneyUtils;
import com.ferramentafacil.app.utils.TextoUtils;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.Source;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Solicitações e aluguéis (coleção "rentals").
 *
 * Regras de negócio garantidas aqui (e reforçadas nas regras do Firestore):
 *  - o valor total é calculado pelo app: valor da diária × quantidade de dias;
 *  - a disponibilidade é conferida no servidor antes de solicitar;
 *  - a aprovação usa TRANSAÇÃO: confere conflitos e reserva o período de forma atômica,
 *    impedindo que duas solicitações aprovadas usem a ferramenta no mesmo período.
 */
public class RentalRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final NotificationRepository notificacoes = new NotificationRepository();

    private String uid() {
        return FirebaseAuth.getInstance().getUid();
    }

    private DocumentReference rentalRef(String id) {
        return db.collection(Constants.COL_RENTALS).document(id);
    }

    // ------------------------------------------------------------------ Solicitar

    public void solicitar(Tool tool, User eu, long retirada, long devolucao, Callback<String> cb) {
        String uid = uid();
        String erro = DateUtils.validarPeriodo(retirada, devolucao);
        if (erro != null) {
            cb.onError(erro);
            return;
        }
        if (uid == null || uid.equals(tool.ownerId)) {
            cb.onError("Você não pode alugar a sua própria ferramenta.");
            return;
        }

        // Lê a versão mais recente da ferramenta direto do servidor
        db.collection(Constants.COL_TOOLS).document(tool.toolId).get(Source.SERVER)
                .addOnSuccessListener(doc -> {
                    Tool atual = doc.toObject(Tool.class);
                    if (atual == null || !atual.disponivel) {
                        cb.onError("Esta ferramenta não está disponível para aluguel no momento.");
                        return;
                    }
                    if (atual.temConflito(retirada, devolucao, null)) {
                        cb.onError("A ferramenta já está reservada em parte desse período. Escolha outras datas.");
                        return;
                    }
                    int dias = DateUtils.calcularDias(retirada, devolucao);

                    DocumentReference ref = db.collection(Constants.COL_RENTALS).document();
                    Rental r = new Rental();
                    r.toolId = atual.toolId;
                    r.toolNome = atual.nome;
                    r.toolImagem = TextoUtils.seguro(atual.primeiraImagem());
                    r.ownerId = atual.ownerId;
                    r.ownerNome = TextoUtils.seguro(tool.ownerNome);
                    r.renterId = uid;
                    r.renterNome = TextoUtils.seguro(eu.nome);
                    r.renterFoto = TextoUtils.seguro(eu.fotoPerfil);
                    r.dataRetirada = retirada;
                    r.dataDevolucao = devolucao;
                    r.quantidadeDias = dias;
                    r.valorDiaria = atual.valorDiaria;
                    r.valorTotal = DateUtils.calcularTotal(atual.valorDiaria, dias);
                    r.status = RentalStatus.PENDENTE;
                    r.dataSolicitacao = System.currentTimeMillis();
                    r.dataAtualizacao = r.dataSolicitacao;
                    r.avaliado = false;

                    WriteBatch batch = db.batch();
                    batch.set(ref, r);
                    // Vincula a conversa (ferramenta + locatário) à solicitação
                    String chatId = ChatRepository.chatId(atual.toolId, uid);
                    batch.set(db.collection(Constants.COL_CHATS).document(chatId),
                            ChatRepository.montarMeta(atual.toolId, atual.nome, r.toolImagem, atual.ownerId,
                                    r.ownerNome, uid, r.renterNome, ref.getId()),
                            SetOptions.merge());
                    // Notifica o proprietário
                    batch.set(notificacoes.novaRef(), NotificationRepository.montar(atual.ownerId,
                            "Nova solicitação de aluguel",
                            r.renterNome + " quer alugar \"" + atual.nome + "\" de "
                                    + DateUtils.formatarData(retirada) + " a " + DateUtils.formatarData(devolucao)
                                    + " (" + MoneyUtils.formatar(r.valorTotal) + ").",
                            Notificacao.TIPO_SOLICITACAO, ref.getId()));

                    batch.commit()
                            .addOnSuccessListener(v -> cb.onSuccess(ref.getId()))
                            .addOnFailureListener(e -> cb.onError(FirebaseErrorUtils.mensagem(e)));
                })
                .addOnFailureListener(e -> cb.onError(FirebaseErrorUtils.mensagem(e)));
    }

    // ------------------------------------------------------------------ Proprietário

    /** Aprova a solicitação reservando o período na ferramenta (transação atômica). */
    public void aprovar(Rental r, Callback<Void> cb) {
        DocumentReference toolRef = db.collection(Constants.COL_TOOLS).document(r.toolId);
        DocumentReference privRef = toolRef.collection(Constants.SUB_PRIVADO).document(Constants.DOC_ENDERECO);
        DocumentReference ref = rentalRef(r.rentalId);

        db.runTransaction(tx -> {
            DocumentSnapshot rentalSnap = tx.get(ref);
            DocumentSnapshot toolSnap = tx.get(toolRef);
            DocumentSnapshot privSnap = tx.get(privRef);

            if (!RentalStatus.PENDENTE.equals(rentalSnap.getString("status")))
                throw FirebaseErrorUtils.regra("Esta solicitação já foi respondida ou cancelada.");
            Tool tool = toolSnap.toObject(Tool.class);
            if (tool == null) throw FirebaseErrorUtils.regra("O anúncio desta ferramenta não existe mais.");
            if (tool.temConflito(r.dataRetirada, r.dataDevolucao, r.rentalId))
                throw FirebaseErrorUtils.regra("Já existe um aluguel aprovado que conflita com este período. "
                        + "Recuse esta solicitação ou converse com o interessado sobre outras datas.");

            List<Map<String, Object>> periodos = new ArrayList<>();
            for (Periodo p : tool.periodosVigentes()) periodos.add(p.paraMapa());
            periodos.add(new Periodo(r.rentalId, r.dataRetirada, r.dataDevolucao).paraMapa());
            tx.update(toolRef, "periodosOcupados", periodos);

            String endereco = privSnap.getString("endereco");
            String cidade = privSnap.getString("cidade");
            Map<String, Object> upd = new HashMap<>();
            upd.put("status", RentalStatus.APROVADA);
            upd.put("enderecoRetirada", (endereco != null ? endereco : "")
                    + (cidade != null && !cidade.isEmpty() ? " — " + cidade : ""));
            upd.put("dataAtualizacao", System.currentTimeMillis());
            tx.update(ref, upd);
            return null;
        }).addOnSuccessListener(v -> {
            notificacoes.enviar(r.renterId, "Solicitação aprovada! ✅",
                    "Seu aluguel de \"" + r.toolNome + "\" foi aprovado. Veja o endereço de retirada em Meus aluguéis.",
                    Notificacao.TIPO_STATUS, r.rentalId);
            cb.onSuccess(null);
        }).addOnFailureListener(e -> cb.onError(FirebaseErrorUtils.mensagem(e)));
    }

    public void recusar(Rental r, Callback<Void> cb) {
        transicao(r, RentalStatus.PENDENTE, RentalStatus.RECUSADA, false, r.renterId,
                "Solicitação recusada",
                "O proprietário recusou a solicitação de \"" + r.toolNome + "\". Que tal procurar outra opção?", cb);
    }

    /** Proprietário confirma que o locatário retirou a ferramenta. */
    public void iniciar(Rental r, Callback<Void> cb) {
        transicao(r, RentalStatus.APROVADA, RentalStatus.EM_ANDAMENTO, false, r.renterId,
                "Aluguel em andamento", "A retirada de \"" + r.toolNome + "\" foi confirmada. Bom trabalho!", cb);
    }

    /** Proprietário confirma a devolução: aluguel concluído e período liberado. */
    public void concluir(Rental r, Callback<Void> cb) {
        transicao(r, RentalStatus.EM_ANDAMENTO, RentalStatus.CONCLUIDA, true, r.renterId,
                "Aluguel concluído",
                "A devolução de \"" + r.toolNome + "\" foi confirmada. Avalie sua experiência!", cb);
    }

    /** Proprietário cancela um aluguel já aprovado (libera o período). */
    public void cancelarPeloProprietario(Rental r, Callback<Void> cb) {
        transicao(r, RentalStatus.APROVADA, RentalStatus.CANCELADA, true, r.renterId,
                "Aluguel cancelado", "O proprietário cancelou o aluguel de \"" + r.toolNome + "\".", cb);
    }

    // ------------------------------------------------------------------ Locatário

    /** Locatário cancela a própria solicitação enquanto ela ainda está pendente. */
    public void cancelarPeloLocatario(Rental r, Callback<Void> cb) {
        transicao(r, RentalStatus.PENDENTE, RentalStatus.CANCELADA, false, r.ownerId,
                "Solicitação cancelada", r.renterNome + " cancelou a solicitação de \"" + r.toolNome + "\".", cb);
    }

    /** Muda o status conferindo o status atual dentro de uma transação. */
    private void transicao(Rental r, String de, String para, boolean liberarPeriodo, String destinatario,
                           String titulo, String mensagem, Callback<Void> cb) {
        DocumentReference ref = rentalRef(r.rentalId);
        DocumentReference toolRef = db.collection(Constants.COL_TOOLS).document(r.toolId);
        db.runTransaction(tx -> {
            DocumentSnapshot snap = tx.get(ref);
            DocumentSnapshot toolSnap = liberarPeriodo ? tx.get(toolRef) : null;
            if (!de.equals(snap.getString("status")))
                throw FirebaseErrorUtils.regra("O status deste aluguel mudou. Atualize a tela e tente novamente.");

            if (toolSnap != null && toolSnap.exists()) {
                Tool tool = toolSnap.toObject(Tool.class);
                if (tool != null) {
                    List<Map<String, Object>> periodos = new ArrayList<>();
                    for (Periodo p : tool.periodosVigentes()) {
                        if (!r.rentalId.equals(p.rentalId)) periodos.add(p.paraMapa());
                    }
                    tx.update(toolRef, "periodosOcupados", periodos);
                }
            }
            tx.update(ref, "status", para, "dataAtualizacao", System.currentTimeMillis());
            return null;
        }).addOnSuccessListener(v -> {
            notificacoes.enviar(destinatario, titulo, mensagem, Notificacao.TIPO_STATUS, r.rentalId);
            cb.onSuccess(null);
        }).addOnFailureListener(e -> cb.onError(FirebaseErrorUtils.mensagem(e)));
    }

    // ------------------------------------------------------------------ Consultas

    public void buscar(String rentalId, Callback<Rental> cb) {
        rentalRef(rentalId).get()
                .addOnSuccessListener(doc -> {
                    Rental r = doc.toObject(Rental.class);
                    if (r == null) cb.onError("Aluguel não encontrado.");
                    else cb.onSuccess(r);
                })
                .addOnFailureListener(e -> cb.onError(FirebaseErrorUtils.mensagem(e)));
    }

    /** Aluguéis em que o usuário é o LOCATÁRIO (atualização em tempo real). */
    public ListenerRegistration escutarComoLocatario(String uid, ListaListener<Rental> l) {
        return escutar(db.collection(Constants.COL_RENTALS).whereEqualTo("renterId", uid), l);
    }

    /** Solicitações recebidas pelo PROPRIETÁRIO (opcionalmente de uma ferramenta específica). */
    public ListenerRegistration escutarComoProprietario(String uid, @Nullable String toolId, ListaListener<Rental> l) {
        Query q = db.collection(Constants.COL_RENTALS).whereEqualTo("ownerId", uid);
        if (toolId != null) q = q.whereEqualTo("toolId", toolId);
        return escutar(q, l);
    }

    private ListenerRegistration escutar(Query q, ListaListener<Rental> l) {
        return q.addSnapshotListener((snap, e) -> {
            if (e != null || snap == null) {
                l.onErro(FirebaseErrorUtils.mensagem(e));
                return;
            }
            List<Rental> lista = snap.toObjects(Rental.class);
            Collections.sort(lista, (a, b) -> Long.compare(b.dataSolicitacao, a.dataSolicitacao));
            l.onLista(lista);
        });
    }

    /** Quantidade de aluguéis concluídos (como locatário + como proprietário). */
    public void contarConcluidos(String uid, Callback<Integer> cb) {
        Task<QuerySnapshot> comoLocatario = db.collection(Constants.COL_RENTALS)
                .whereEqualTo("renterId", uid).whereEqualTo("status", RentalStatus.CONCLUIDA).get();
        Task<QuerySnapshot> comoProprietario = db.collection(Constants.COL_RENTALS)
                .whereEqualTo("ownerId", uid).whereEqualTo("status", RentalStatus.CONCLUIDA).get();
        Tasks.whenAllSuccess(comoLocatario, comoProprietario)
                .addOnSuccessListener(r -> {
                    int total = 0;
                    for (Object o : r) total += ((QuerySnapshot) o).size();
                    cb.onSuccess(total);
                })
                .addOnFailureListener(e -> cb.onError(FirebaseErrorUtils.mensagem(e)));
    }
}
