from collections import Counter
import os


os.environ.setdefault("LOKY_MAX_CPU_COUNT", str(os.cpu_count() or 1))

DEFAULT_EMBEDDING_MODEL_NAME = "sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2"
DEFAULT_CLUSTER_COUNT = None
MIN_CLUSTER_COUNT = 4
MAX_CLUSTER_COUNT = 18
REPRESENTATIVE_COUNT = 3
STANCE_ORDER = ("PRO", "CON", "NEUTRAL")
STOP_WORDS = [
    "있습니다",
    "합니다",
    "됩니다",
    "것입니다",
    "필요합니다",
    "때문입니다",
    "수",
    "더",
    "그",
    "이",
    "저",
    "때",
    "경우",
    "문제",
    "방식",
    "정도",
    "수준",
    "대한",
    "통해",
    "위한",
    "있는",
    "없는",
    "하면",
    "해야",
    "그리고",
    "하지만",
    "또한",
]
KOREAN_SUFFIXES = (
    "입니다",
    "합니다",
    "됩니다",
    "이라는",
    "라는",
    "으로",
    "에서",
    "에게",
    "에는",
    "이나",
    "거나",
    "까지",
    "부터",
    "보다",
    "처럼",
    "은",
    "는",
    "이",
    "가",
    "을",
    "를",
    "의",
    "에",
    "도",
)
_SENTENCE_TRANSFORMER_MODEL = None
_SENTENCE_TRANSFORMER_MODEL_NAME = None
_SENTENCE_TRANSFORMER_LOAD_FAILED = False


def build_clustered_debate_input(debate, cluster_count=DEFAULT_CLUSTER_COUNT):
    speeches = _valid_speeches(debate.get("speeches", []))
    room = debate.get("room", {})

    if not speeches:
        return {
            "topic": room.get("topic", ""),
            "description": room.get("description", ""),
            "totalTurns": 0,
            "stanceCounts": _count_stances([]),
            "clusterMeta": _cluster_meta(
                strategy="empty",
                selected_cluster_count=0,
                candidate_scores=[],
                grouping="stance_then_topic",
            ),
            "clusters": [],
        }

    if cluster_count is None:
        clusters, cluster_meta = _build_stance_then_topic_clusters(speeches)
    else:
        clusters, cluster_meta = _build_global_topic_clusters(speeches, cluster_count)

    return {
        "topic": room.get("topic", ""),
        "description": room.get("description", ""),
        "totalTurns": len(speeches),
        "stanceCounts": _count_stances(speeches),
        "clusterMeta": cluster_meta,
        "clusters": clusters,
    }


def _build_stance_then_topic_clusters(speeches):
    clusters = []
    group_scores = []

    for stance in STANCE_ORDER:
        stance_speeches = [
            speech
            for speech in speeches
            if speech.get("stance") == stance
        ]
        if not stance_speeches:
            continue

        group_clusters, group_meta = _build_topic_clusters(
            speeches=stance_speeches,
            cluster_count=None,
            stance_group=stance,
        )
        clusters.extend(group_clusters)
        group_scores.append(
            {
                "stance": stance,
                "speechCount": len(stance_speeches),
                "selectedClusterCount": len(group_clusters),
                "strategy": group_meta["clusterCountStrategy"],
                "embeddingBackend": group_meta.get("embeddingBackend"),
                "embeddingModel": group_meta.get("embeddingModel"),
                "candidateScores": group_meta["candidateScores"],
            }
        )

    _renumber_clusters(clusters)
    first_group_score = group_scores[0] if group_scores else {}
    embedding_backend = first_group_score.get("embeddingBackend")
    embedding_model = first_group_score.get("embeddingModel")
    return clusters, _cluster_meta(
        strategy="stance_then_topic",
        selected_cluster_count=len(clusters),
        candidate_scores=[],
        grouping="stance_then_topic",
        group_scores=group_scores,
        algorithm=_algorithm_name(embedding_backend),
        embedding_backend=embedding_backend,
        embedding_model=embedding_model,
    )


def _build_global_topic_clusters(speeches, cluster_count):
    clusters, cluster_meta = _build_topic_clusters(
        speeches=speeches,
        cluster_count=cluster_count,
        stance_group=None,
    )
    cluster_meta["grouping"] = "global_topic"
    return clusters, cluster_meta


def _build_topic_clusters(speeches, cluster_count, stance_group):
    embedding, vectors = _embed_contents(speeches)
    labels, centers, cluster_meta = _cluster_vectors(vectors, cluster_count)
    cluster_meta["algorithm"] = _algorithm_name(embedding["backend"])
    cluster_meta["embeddingBackend"] = embedding["backend"]
    cluster_meta["embeddingModel"] = embedding["model_name"]
    clusters = _build_clusters(
        speeches=speeches,
        vectors=vectors,
        labels=labels,
        centers=centers,
        label_vectors=embedding["label_vectors"],
        label_vectorizer=embedding["label_vectorizer"],
        stance_group=stance_group,
    )
    return clusters, cluster_meta


def _valid_speeches(speeches):
    return [
        speech
        for speech in speeches
        if speech.get("content") and speech.get("stance") in STANCE_ORDER
    ]


def _count_stances(speeches):
    counts = Counter(speech["stance"] for speech in speeches)
    return {stance: counts.get(stance, 0) for stance in STANCE_ORDER}


def _embed_contents(speeches):
    try:
        from sklearn.feature_extraction.text import TfidfVectorizer
    except ImportError as exc:
        raise RuntimeError(
            "scikit-learn is not installed. Run install_dependencies.py in the nbe911 environment."
        ) from exc

    label_vectorizer = TfidfVectorizer(
        token_pattern=r"(?u)\b[0-9A-Za-z가-힣]{2,}\b",
        ngram_range=(1, 2),
        max_features=800,
        sublinear_tf=True,
        stop_words=STOP_WORDS,
    )
    contents = [_embedding_text(speech) for speech in speeches]
    vectors, backend, model_name = _embed_for_clustering(contents)
    label_vectors = label_vectorizer.fit_transform(contents)
    return {
        "backend": backend,
        "model_name": model_name,
        "label_vectorizer": label_vectorizer,
        "label_vectors": label_vectors,
    }, vectors


def _embed_for_clustering(contents):
    preferred_backend = os.getenv("AI_REPORT_EMBEDDING_BACKEND", "sentence_transformer")
    if preferred_backend not in ("sentence_transformer", "tfidf"):
        raise ValueError("AI_REPORT_EMBEDDING_BACKEND must be sentence_transformer or tfidf")

    if preferred_backend == "sentence_transformer":
        sentence_vectors = _try_embed_with_sentence_transformer(contents)
        if sentence_vectors is not None:
            return sentence_vectors

    return _embed_with_tfidf(contents)


def _try_embed_with_sentence_transformer(contents):
    model = _get_sentence_transformer_model()
    if model is None:
        return None

    try:
        vectors = model.encode(
            contents,
            batch_size=int(os.getenv("AI_REPORT_EMBEDDING_BATCH_SIZE", "32")),
            convert_to_numpy=True,
            normalize_embeddings=True,
            show_progress_bar=False,
        )
    except Exception as exc:
        print(f"Sentence-BERT 임베딩 실패: {exc}", flush=True)
        print("TF-IDF 클러스터링으로 fallback합니다.", flush=True)
        return None

    return vectors, "sentence_transformer", _SENTENCE_TRANSFORMER_MODEL_NAME


def _get_sentence_transformer_model():
    global _SENTENCE_TRANSFORMER_LOAD_FAILED
    global _SENTENCE_TRANSFORMER_MODEL
    global _SENTENCE_TRANSFORMER_MODEL_NAME

    if _SENTENCE_TRANSFORMER_MODEL is not None:
        return _SENTENCE_TRANSFORMER_MODEL
    if _SENTENCE_TRANSFORMER_LOAD_FAILED:
        return None

    model_name = os.getenv("AI_REPORT_EMBEDDING_MODEL", DEFAULT_EMBEDDING_MODEL_NAME)
    try:
        from sentence_transformers import SentenceTransformer
    except ImportError:
        print("sentence-transformers 미설치: TF-IDF 클러스터링으로 fallback합니다.", flush=True)
        _SENTENCE_TRANSFORMER_LOAD_FAILED = True
        return None

    try:
        _SENTENCE_TRANSFORMER_MODEL = SentenceTransformer(
            model_name,
            device=_sentence_transformer_device(),
        )
        _SENTENCE_TRANSFORMER_MODEL_NAME = model_name
        return _SENTENCE_TRANSFORMER_MODEL
    except Exception as exc:
        print(f"Sentence-BERT 모델 로딩 실패: {exc}", flush=True)
        print("TF-IDF 클러스터링으로 fallback합니다.", flush=True)
        _SENTENCE_TRANSFORMER_LOAD_FAILED = True
        return None


def _sentence_transformer_device():
    try:
        import torch
    except ImportError:
        return "cpu"

    return "cuda" if torch.cuda.is_available() else "cpu"


def _embed_with_tfidf(contents):
    try:
        from sklearn.feature_extraction.text import TfidfVectorizer
        from sklearn.pipeline import FeatureUnion
    except ImportError as exc:
        raise RuntimeError(
            "scikit-learn is not installed. Run install_dependencies.py in the nbe911 environment."
        ) from exc

    # Sentence-BERT를 사용할 수 없을 때만 쓰는 fallback입니다.
    # 단어 n-gram은 쟁점 키워드를, 문자 n-gram은 한국어 조사/띄어쓰기 차이를 보완합니다.
    word_vectorizer = TfidfVectorizer(
        token_pattern=r"(?u)\b[0-9A-Za-z가-힣]{2,}\b",
        ngram_range=(1, 2),
        max_features=1600,
        sublinear_tf=True,
        stop_words=STOP_WORDS,
    )
    char_vectorizer = TfidfVectorizer(
        analyzer="char_wb",
        ngram_range=(3, 5),
        max_features=2200,
        sublinear_tf=True,
    )
    vectorizer = FeatureUnion(
        [
            ("word", word_vectorizer),
            ("char", char_vectorizer),
        ]
    )
    vectors = vectorizer.fit_transform(contents)
    return vectors, "tfidf", "word_ngram+char_ngram_tfidf"


def _embedding_text(speech):
    keywords = " ".join(speech.get("keywords", []))
    # 원문 content는 그대로 두고, keywords만 여러 번 더해 짧은 핵심어가 클러스터링에 묻히지 않게 합니다.
    return f"{speech['content']} {keywords} {keywords} {keywords}".lower()


def _cluster_vectors(vectors, cluster_count):
    speech_count = vectors.shape[0]
    if speech_count <= 1:
        return [0 for _ in range(speech_count)], vectors[:1], _cluster_meta(
            strategy="single_item",
            selected_cluster_count=speech_count,
            candidate_scores=[],
        )

    try:
        from sklearn.cluster import KMeans
        from sklearn.metrics import silhouette_score
    except ImportError as exc:
        raise RuntimeError(
            "scikit-learn is not installed. Run install_dependencies.py in the nbe911 environment."
        ) from exc

    if cluster_count is not None:
        selected_cluster_count = max(1, min(cluster_count, speech_count))
        model = _fit_kmeans(KMeans, vectors, selected_cluster_count)
        return model.labels_, model.cluster_centers_, _cluster_meta(
            strategy="fixed",
            selected_cluster_count=selected_cluster_count,
            candidate_scores=[],
        )

    if speech_count <= 3:
        model = _fit_kmeans(KMeans, vectors, speech_count)
        return model.labels_, model.cluster_centers_, _cluster_meta(
            strategy="small_dataset",
            selected_cluster_count=speech_count,
            candidate_scores=[],
        )

    # 데이터마다 의견 다양성이 다르므로 여러 k를 비교해 자동으로 클러스터 수를 선택합니다.
    # silhouette score는 같은 클러스터끼리는 가깝고 다른 클러스터와는 멀수록 높아집니다.
    best_model = None
    best_score = None
    candidate_scores = []
    for candidate_count in _candidate_cluster_counts(speech_count):
        model = _fit_kmeans(KMeans, vectors, candidate_count)
        score = _score_cluster_choice(vectors, model.labels_, silhouette_score)
        candidate_scores.append(
            {
                "clusterCount": candidate_count,
                "score": round(score, 4),
            }
        )
        if best_score is None or score > best_score:
            best_score = score
            best_model = model

    return best_model.labels_, best_model.cluster_centers_, _cluster_meta(
        strategy="adaptive_silhouette",
        selected_cluster_count=len(best_model.cluster_centers_),
        candidate_scores=candidate_scores,
    )


def _candidate_cluster_counts(speech_count):
    if speech_count <= 2:
        return [speech_count]

    dynamic_upper_bound = max(MIN_CLUSTER_COUNT, round((speech_count ** 0.5) * 2))
    upper_bound = min(MAX_CLUSTER_COUNT, dynamic_upper_bound, speech_count - 1)
    # 발화가 적은 경우는 가능한 범위를 넓게 보고, 발화가 많으면 데이터 규모에 맞춰 후보 범위를 늘립니다.
    lower_bound = 2 if speech_count < MIN_CLUSTER_COUNT * 2 else MIN_CLUSTER_COUNT
    lower_bound = min(lower_bound, upper_bound)
    return list(range(lower_bound, upper_bound + 1))


def _fit_kmeans(kmeans_class, vectors, cluster_count):
    return kmeans_class(
        n_clusters=cluster_count,
        random_state=42,
        n_init=10,
    ).fit(vectors)


def _score_cluster_choice(vectors, labels, silhouette_score):
    label_counts = Counter(labels)
    if len(label_counts) <= 1:
        return -1

    silhouette = silhouette_score(vectors, labels, metric="cosine")
    singleton_count = sum(1 for count in label_counts.values() if count == 1)
    tiny_cluster_count = sum(1 for count in label_counts.values() if count <= 2)

    # 너무 잘게 쪼개진 클러스터는 대표 발언이 빈약해지므로 약한 페널티를 줍니다.
    return silhouette - (singleton_count * 0.03) - (tiny_cluster_count * 0.01)


def _cluster_meta(
    strategy,
    selected_cluster_count,
    candidate_scores,
    grouping="global_topic",
    group_scores=None,
    algorithm="sentence_embedding+kmeans",
    embedding_backend=None,
    embedding_model=None,
):
    meta = {
        "algorithm": algorithm,
        "labelExtractor": "word_ngram_tfidf",
        "grouping": grouping,
        "clusterCountStrategy": strategy,
        "selectedClusterCount": selected_cluster_count,
        "candidateScores": candidate_scores,
    }
    if embedding_backend is not None:
        meta["embeddingBackend"] = embedding_backend
    if embedding_model is not None:
        meta["embeddingModel"] = embedding_model
    if group_scores is not None:
        meta["groupScores"] = group_scores
    return meta


def _algorithm_name(embedding_backend):
    if embedding_backend == "tfidf":
        return "tfidf+kmeans"
    return "sentence_embedding+kmeans"


def _build_clusters(
    speeches,
    vectors,
    labels,
    centers,
    label_vectors,
    label_vectorizer,
    stance_group,
):
    clusters = []
    for cluster_id in range(len(centers)):
        member_indexes = [index for index, label in enumerate(labels) if label == cluster_id]
        if not member_indexes:
            continue

        member_speeches = [speeches[index] for index in member_indexes]
        representative_indexes = _representative_indexes(
            cluster_id=cluster_id,
            member_indexes=member_indexes,
            vectors=vectors,
            centers=centers,
        )
        keywords = _top_keywords(member_indexes, label_vectors, label_vectorizer)
        stance_distribution = _count_stances(member_speeches)

        clusters.append(
            {
                "clusterId": 0,
                "stanceGroup": stance_group or _dominant_stance(stance_distribution),
                "label": _build_label(keywords),
                "memberCount": len(member_indexes),
                "stanceDistribution": stance_distribution,
                "keywords": keywords,
                "summary": _build_summary_hint(stance_distribution, keywords),
                "representativeOpinions": [
                    _format_representative_opinion(speeches[index])
                    for index in representative_indexes
                ],
            }
        )

    clusters.sort(key=lambda cluster: (-cluster["memberCount"], cluster["label"]))
    _renumber_clusters(clusters)

    return clusters


def _renumber_clusters(clusters):
    for index, cluster in enumerate(clusters, start=1):
        cluster["clusterId"] = index


def _dominant_stance(stance_distribution):
    return max(STANCE_ORDER, key=lambda stance: stance_distribution.get(stance, 0))


def _representative_indexes(cluster_id, member_indexes, vectors, centers):
    try:
        from sklearn.metrics import pairwise_distances
    except ImportError as exc:
        raise RuntimeError(
            "scikit-learn is not installed. Run install_dependencies.py in the nbe911 environment."
        ) from exc

    center = centers[cluster_id].reshape(1, -1)
    member_vectors = vectors[member_indexes]
    distances = pairwise_distances(member_vectors, center, metric="cosine").ravel()
    selected_positions = []
    remaining_positions = list(range(len(member_indexes)))

    while remaining_positions and len(selected_positions) < REPRESENTATIVE_COUNT:
        if not selected_positions:
            best_position = min(
                remaining_positions,
                key=lambda position: (distances[position], member_indexes[position]),
            )
        else:
            selected_vectors = member_vectors[selected_positions]
            diversity_distances = pairwise_distances(
                member_vectors[remaining_positions],
                selected_vectors,
                metric="cosine",
            )
            nearest_selected_distances = diversity_distances.min(axis=1)
            ranked_positions = sorted(
                zip(remaining_positions, nearest_selected_distances),
                key=lambda item: (
                    distances[item[0]] - (item[1] * 0.25),
                    member_indexes[item[0]],
                ),
            )
            best_position = ranked_positions[0][0]

        selected_positions.append(best_position)
        remaining_positions.remove(best_position)

    return [member_indexes[position] for position in selected_positions]


def _top_keywords(member_indexes, vectors, vectorizer, limit=4):
    feature_names = vectorizer.get_feature_names_out()
    cluster_vector = vectors[member_indexes].sum(axis=0)
    scores = cluster_vector.A1
    ranked_indexes = scores.argsort()[::-1]
    keywords = []

    for index in ranked_indexes:
        keyword = _clean_keyword(feature_names[index])
        if len(keyword) < 2:
            continue
        if keyword in STOP_WORDS or keyword in keywords:
            continue
        keywords.append(keyword)
        if len(keywords) == limit:
            break

    return keywords


def _clean_keyword(keyword):
    cleaned_tokens = []
    for token in keyword.strip().split():
        cleaned = token
        for suffix in KOREAN_SUFFIXES:
            if len(cleaned) > len(suffix) + 1 and cleaned.endswith(suffix):
                cleaned = cleaned[: -len(suffix)]
                break
        if len(cleaned) >= 2 and cleaned not in STOP_WORDS:
            cleaned_tokens.append(cleaned)
    return " ".join(cleaned_tokens)


def _build_label(keywords):
    if not keywords:
        return "주요 의견 묶음"
    return " / ".join(keywords[:3])


def _build_summary_hint(stance_distribution, keywords):
    dominant_stance = max(STANCE_ORDER, key=lambda stance: stance_distribution.get(stance, 0))
    stance_text = {
        "PRO": "찬성 의견이 많은 묶음",
        "CON": "반대 의견이 많은 묶음",
        "NEUTRAL": "중립 또는 절충 의견이 많은 묶음",
    }[dominant_stance]
    keyword_text = ", ".join(keywords) if keywords else "핵심어 없음"
    return f"{stance_text}입니다. 주요어: {keyword_text}."


def _format_representative_opinion(speech):
    return (
        f"[{speech.get('turnIndex')}] "
        f"{speech.get('speaker')} / {speech.get('stance')}: "
        f"{speech.get('content', '')}"
    )
