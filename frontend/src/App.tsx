import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { useState } from "react";

type Category = "SOCCER" | "BASEBALL" | "BASKETBALL" | "VOLLEYBALL";

interface RelatedArticle {
  article_id: number;
  title: string;
  link: string;
  originallink?: string | null;
  pub_date: string;
}

interface SummaryResponse {
  category: string;
  keyword: string;
  fromDatetime: string;
  toDatetime: string;
  summaryTitle: string | null;
  summaryText: string;
  // 백엔드에서 JSON 문자열로 내려오는 relatedArticles
  relatedArticles: string;
}

const CATEGORY_LABELS: { key: Category; label: string; keyword: string }[] = [
  { key: "SOCCER", label: "축구", keyword: "축구" },
  { key: "BASEBALL", label: "야구", keyword: "야구" },
  { key: "BASKETBALL", label: "농구", keyword: "농구" },
  { key: "VOLLEYBALL", label: "배구", keyword: "배구" }
];

async function fetchSummary(category: Category, keyword: string) {
  const res = await axios.get<SummaryResponse>("/api/news/summary", {
    params: { category, keyword }
  });
  return res.data;
}

async function fetchAndSummarize() {
  const res = await axios.post<{ ok: boolean; message?: string }>("/api/admin/news/fetch-and-summarize");
  return res.data;
}

function formatAxiosError(err: unknown) {
  if (!axios.isAxiosError(err)) return String(err);
  const status = err.response?.status;
  const data = err.response?.data;
  return [
    status ? `HTTP ${status}` : "",
    data ? (typeof data === "string" ? data : JSON.stringify(data, null, 2)) : err.message
  ]
    .filter(Boolean)
    .join("\n");
}

export function App() {
  const [selected, setSelected] = useState<{ category: Category; keyword: string } | null>(null);
  const qc = useQueryClient();

  const summaryQuery = useQuery({
    queryKey: ["summary", selected?.category, selected?.keyword],
    queryFn: () => fetchSummary(selected!.category, selected!.keyword),
    enabled: !!selected
  });

  const adminMutation = useMutation({
    mutationFn: fetchAndSummarize,
    onSuccess: async () => {
      await qc.invalidateQueries({ queryKey: ["summary"] });
    }
  });

  return (
    <div style={{ maxWidth: 960, margin: "0 auto", padding: "24px", fontFamily: "system-ui" }}>
      <h1 style={{ fontSize: 28, marginBottom: 8 }}>스포츠 뉴스 요약</h1>
      <p style={{ color: "#666", marginBottom: 24 }}>
        최근 2일간의 이슈를 간단히 요약해서 보여줍니다. 종목을 선택해 보세요.
      </p>

      <div
        style={{
          display: "flex",
          alignItems: "center",
          gap: 12,
          padding: 16,
          border: "1px solid #e5e7eb",
          borderRadius: 12,
          marginBottom: 16,
          background: "#fff"
        }}
      >
        <button
          onClick={() => adminMutation.mutate()}
          disabled={adminMutation.isPending}
          style={{
            padding: "10px 14px",
            borderRadius: 10,
            border: "1px solid #d1d5db",
            background: adminMutation.isPending ? "#f3f4f6" : "#111827",
            color: adminMutation.isPending ? "#111827" : "#fff",
            cursor: adminMutation.isPending ? "not-allowed" : "pointer",
            fontSize: 14
          }}
        >
          {adminMutation.isPending ? "수집/요약 실행 중..." : "수집/요약 지금 실행"}
        </button>
        <span style={{ color: "#6b7280", fontSize: 13 }}>
          버튼을 누르면 네이버에서 수집 → DB 저장 → 최근 2일 요약 생성까지 한 번에 수행합니다.
        </span>
      </div>

      {(adminMutation.isError || adminMutation.data?.ok === false) && (
        <pre
          style={{
            marginBottom: 16,
            padding: 12,
            borderRadius: 12,
            border: "1px solid #fecaca",
            background: "#fff1f2",
            color: "#991b1b",
            overflowX: "auto",
            fontSize: 12
          }}
        >
          {adminMutation.error ? formatAxiosError(adminMutation.error) : adminMutation.data?.message}
        </pre>
      )}

      <div style={{ display: "flex", gap: 12, marginBottom: 24, flexWrap: "wrap" }}>
        {CATEGORY_LABELS.map((c) => (
          <button
            key={c.key}
            onClick={() => setSelected({ category: c.key, keyword: c.keyword })}
            style={{
              padding: "10px 18px",
              borderRadius: 999,
              border: "1px solid",
              borderColor: selected?.category === c.key ? "#2563eb" : "#d1d5db",
              backgroundColor: selected?.category === c.key ? "#2563eb" : "#ffffff",
              color: selected?.category === c.key ? "#ffffff" : "#111827",
              cursor: "pointer",
              fontSize: 14
            }}
          >
            {c.label}
          </button>
        ))}
      </div>

      {!selected && <p>종목 버튼을 눌러 요약을 확인하세요.</p>}

      {selected && summaryQuery.isLoading && <p>요약을 불러오는 중입니다...</p>}

      {selected && summaryQuery.isError && (
        <pre
          style={{
            padding: 12,
            borderRadius: 12,
            border: "1px solid #fecaca",
            background: "#fff1f2",
            color: "#991b1b",
            overflowX: "auto",
            fontSize: 12
          }}
        >
          {formatAxiosError(summaryQuery.error)}
        </pre>
      )}

      {selected && !summaryQuery.isLoading && !summaryQuery.isError && summaryQuery.data && (
        <div
          style={{
            borderRadius: 16,
            border: "1px solid #e5e7eb",
            padding: 20,
            backgroundColor: "#fafafa",
            marginTop: 8
          }}
        >
          <h2 style={{ fontSize: 20, marginBottom: 8 }}>
            {summaryQuery.data.summaryTitle || `${summaryQuery.data.keyword} 최근 2일 이슈 요약`}
          </h2>
          <p style={{ fontSize: 14, color: "#6b7280", marginBottom: 12 }}>
            {new Date(summaryQuery.data.fromDatetime).toLocaleString()} ~{" "}
            {new Date(summaryQuery.data.toDatetime).toLocaleString()}
          </p>
          {/* 간단 텍스트 요약 */}
          <p style={{ whiteSpace: "pre-line", lineHeight: 1.6, marginBottom: 16 }}>
            {summaryQuery.data.summaryText}
          </p>

          {/* 기사별 카드 리스트 (제목 / 설명 / 링크 분리) */}
          {(() => {
            let articles: RelatedArticle[] = [];
            try {
              articles = JSON.parse(summaryQuery.data.relatedArticles) as RelatedArticle[];
            } catch {
              // 파싱 실패 시에는 카드 렌더링 생략
            }

            if (!articles.length) return null;

            return (
              <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
                {articles.map((a) => (
                  <div
                    key={a.article_id}
                    style={{
                      borderRadius: 12,
                      border: "1px solid #e5e7eb",
                      padding: 12,
                      background: "#ffffff"
                    }}
                  >
                    <div
                      style={{
                        display: "flex",
                        justifyContent: "space-between",
                        alignItems: "baseline",
                        gap: 8,
                        marginBottom: 4
                      }}
                    >
                      <h3
                        style={{
                          fontSize: 15,
                          fontWeight: 600,
                          margin: 0,
                          flex: 1,
                          lineHeight: 1.4
                        }}
                      >
                        {a.title}
                      </h3>
                      <a
                        href={a.link || a.originallink || "#"}
                        target="_blank"
                        rel="noreferrer"
                        style={{
                          fontSize: 12,
                          color: "#2563eb",
                          textDecoration: "none",
                          whiteSpace: "nowrap"
                        }}
                      >
                        기사 보기 ↗
                      </a>
                    </div>
                    <p
                      style={{
                        fontSize: 13,
                        color: "#4b5563",
                        margin: 0,
                        lineHeight: 1.5
                      }}
                    >
                      {new Date(a.pub_date).toLocaleString()}
                    </p>
                  </div>
                ))}
              </div>
            );
          })()}
        </div>
      )}
    </div>
  );
}

