from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER, TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, PageBreak, Table, TableStyle
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont


OUTPUT = "output/pdf/Dennis_MST_2008_Korean_Detailed_Summary.pdf"
FONT_PATH = "/System/Library/Fonts/Supplemental/AppleGothic.ttf"


def register_fonts():
    pdfmetrics.registerFont(TTFont("Korean", FONT_PATH))
    pdfmetrics.registerFont(TTFont("Korean-Bold", FONT_PATH))


def p(text, style):
    return Paragraph(text.replace("\n", "<br/>"), style)


def bullet(text, style):
    return p("- " + text, style)


def add_page_number(canvas, doc):
    canvas.saveState()
    canvas.setFont("Korean", 8)
    canvas.setFillColor(colors.HexColor("#666666"))
    canvas.drawRightString(200 * mm, 10 * mm, f"{doc.page}")
    canvas.restoreState()


register_fonts()

styles = getSampleStyleSheet()
styles.add(ParagraphStyle(
    name="TitleKo",
    fontName="Korean-Bold",
    fontSize=20,
    leading=28,
    alignment=TA_CENTER,
    spaceAfter=12,
))
styles.add(ParagraphStyle(
    name="SubtitleKo",
    fontName="Korean",
    fontSize=11,
    leading=17,
    alignment=TA_CENTER,
    textColor=colors.HexColor("#555555"),
    spaceAfter=18,
))
styles.add(ParagraphStyle(
    name="H1Ko",
    fontName="Korean-Bold",
    fontSize=15,
    leading=22,
    textColor=colors.HexColor("#1F2937"),
    spaceBefore=10,
    spaceAfter=8,
))
styles.add(ParagraphStyle(
    name="H2Ko",
    fontName="Korean-Bold",
    fontSize=12,
    leading=18,
    textColor=colors.HexColor("#374151"),
    spaceBefore=8,
    spaceAfter=5,
))
styles.add(ParagraphStyle(
    name="BodyKo",
    fontName="Korean",
    fontSize=10,
    leading=16,
    alignment=TA_LEFT,
    spaceAfter=5,
))
styles.add(ParagraphStyle(
    name="BulletKo",
    fontName="Korean",
    fontSize=9.6,
    leading=15,
    leftIndent=10,
    firstLineIndent=-7,
    spaceAfter=3,
))
styles.add(ParagraphStyle(
    name="SmallKo",
    fontName="Korean",
    fontSize=8.5,
    leading=13,
    textColor=colors.HexColor("#666666"),
    spaceAfter=4,
))

story = []

story.append(p("Media Synchronicity Theory<br/>한국어 상세 요약", styles["TitleKo"]))
story.append(p(
    "Dennis, A. R., Fuller, R. M., & Valacich, J. S. (2008). "
    "Media, Tasks, and Communication Processes: A Theory of Media Synchronicity. MIS Quarterly, 32(3), 575-600.",
    styles["SubtitleKo"],
))
story.append(p(
    "주의: 이 문서는 원문 전체 번역이 아니라, 논문 전체 흐름을 한국어로 풀어쓴 상세 요약입니다. "
    "원문의 긴 문장이나 표를 그대로 재현하지 않고 핵심 개념, 논리 구조, 서비스 적용 가능성을 중심으로 정리했습니다.",
    styles["SmallKo"],
))

story.append(p("1. 한 줄 결론", styles["H1Ko"]))
story.append(p(
    "이 논문의 핵심은 ‘좋은 커뮤니케이션 매체는 하나로 정해지는 것이 아니라, 수행하려는 커뮤니케이션 과정에 맞아야 한다’는 것이다. "
    "정보를 전달하고 각자가 이해하는 과정에는 낮은 동시성이 유리하고, 서로의 의미를 맞추고 합의하는 과정에는 높은 동시성이 유리하다. "
    "대부분의 협업 과제는 두 과정이 모두 필요하므로, 하나의 매체만 쓰기보다 여러 매체를 조합하는 것이 더 좋은 성과를 만든다는 것이 논문의 큰 주장이다.",
    styles["BodyKo"],
))

story.append(p("2. 논문이 다루는 문제", styles["H1Ko"]))
story.append(p(
    "기존의 Media Richness Theory는 대체로 ‘복잡하고 모호한 과제에는 풍부한 매체, 단순한 과제에는 덜 풍부한 매체가 적합하다’고 설명했다. "
    "하지만 이메일, 그룹웨어, 채팅, 화상회의처럼 새로운 협업 매체가 등장하면서 이 설명만으로는 실제 성과를 충분히 설명하기 어려워졌다. "
    "예를 들어 어떤 상황에서는 빠른 대화보다 문서 기반 비동기 커뮤니케이션이 더 좋은 결과를 만들고, 반대로 어떤 상황에서는 즉각적인 대화가 꼭 필요하다.",
    styles["BodyKo"],
))
story.append(p(
    "Dennis, Fuller, Valacich는 이 한계를 보완하기 위해 매체를 단순히 ‘풍부하다/빈약하다’로 보지 않고, "
    "매체가 정보 전달과 정보 처리 과정을 어떻게 지원하는지로 분석한다. 그래서 논문은 매체 자체보다 ‘커뮤니케이션 과정’과 ‘과제 특성’의 적합성에 초점을 둔다.",
    styles["BodyKo"],
))

story.append(p("3. 핵심 개념: 동시성", styles["H1Ko"]))
story.append(p(
    "Media Synchronicity Theory에서 동시성은 사람들이 같은 시간 흐름 안에서 서로의 메시지에 맞춰 반응하고, "
    "공동의 커뮤니케이션 패턴을 만들어낼 수 있는 정도를 의미한다. 여기서 중요한 점은 단순히 ‘실시간인가 아닌가’가 아니다. "
    "실시간 채팅이라도 메시지가 너무 많아 흐름을 따라가기 어렵다면 동시성이 떨어질 수 있고, 문서 기반 커뮤니케이션도 명확한 절차와 빠른 피드백이 있으면 일부 동시성을 확보할 수 있다.",
    styles["BodyKo"],
))
story.append(p(
    "즉 동시성은 매체의 기술적 속성뿐 아니라 사람들이 그 매체를 어떻게 사용하는지에 의해 결정된다. "
    "논문은 이를 설명하기 위해 매체 능력과 사용자 appropriation, 즉 실제 사용 방식의 중요성을 함께 다룬다.",
    styles["BodyKo"],
))

story.append(p("4. 커뮤니케이션 과정 1: Conveyance", styles["H1Ko"]))
story.append(p(
    "Conveyance는 정보를 전달하고, 수신자가 그 정보를 처리해 자신의 이해를 형성하는 과정이다. "
    "새로운 자료를 읽거나, 근거를 검토하거나, 복잡한 정보를 각자 해석하는 과정이 여기에 해당한다. "
    "이 과정에서는 빠른 반응보다 충분한 검토 시간이 중요하다.",
    styles["BodyKo"],
))
story.append(bullet(
    "예: 긴 보고서 읽기, 근거 링크 검토, 여러 의견을 비교하며 자기 입장 정리하기.",
    styles["BulletKo"],
))
story.append(bullet(
    "적합한 매체: 문서, 게시글, 이메일, 저장된 토론 기록처럼 다시 읽고 천천히 처리할 수 있는 매체.",
    styles["BulletKo"],
))
story.append(bullet(
    "왜 낮은 동시성이 유리한가: 즉각 반응을 요구하지 않기 때문에 사용자가 정보를 곱씹고, 필요한 경우 다시 확인할 수 있다.",
    styles["BulletKo"],
))

story.append(p("5. 커뮤니케이션 과정 2: Convergence", styles["H1Ko"]))
story.append(p(
    "Convergence는 참여자들이 서로의 해석을 맞추고, 의미를 조율하고, 공통된 이해나 합의에 가까워지는 과정이다. "
    "상대가 내 말을 어떻게 이해했는지 확인하고, 오해를 바로잡고, 쟁점을 좁히는 활동이 여기에 해당한다. "
    "이 과정에서는 빠른 피드백과 상호 반응이 중요하다.",
    styles["BodyKo"],
))
story.append(bullet(
    "예: 회의 중 질문과 답변, 토론 중 반박과 재반박, 쟁점 정리, 합의 도출.",
    styles["BulletKo"],
))
story.append(bullet(
    "적합한 매체: 대면 회의, 화상회의, 실시간 채팅, 실시간 발언권처럼 빠른 상호작용을 제공하는 매체.",
    styles["BulletKo"],
))
story.append(bullet(
    "왜 높은 동시성이 유리한가: 상대 반응을 바로 확인하고, 의미 차이를 즉시 조정할 수 있기 때문이다.",
    styles["BulletKo"],
))

story.append(p("6. 다섯 가지 매체 능력", styles["H1Ko"]))
cap_data = [
    ["매체 능력", "의미", "커뮤니케이션에 주는 영향"],
    ["Symbol sets", "정보를 표현하는 방식의 종류", "텍스트, 음성, 이미지, 영상 등 표현 방식이 다양할수록 더 많은 단서를 전달할 수 있다."],
    ["Parallelism", "동시에 여러 메시지를 주고받을 수 있는 정도", "동시 발화가 많으면 정보량은 늘지만, 합의 과정에서는 주의가 분산될 수 있다."],
    ["Transmission velocity", "메시지가 전달되고 응답되는 속도", "빠를수록 실시간 조율과 즉각 피드백에 유리하다."],
    ["Rehearsability", "보내기 전에 메시지를 다듬을 수 있는 정도", "정확한 근거 제시나 신중한 의견 작성에 유리하다."],
    ["Reprocessability", "받은 메시지를 다시 볼 수 있는 정도", "복잡한 정보 검토, 기록 기반 학습, 사후 확인에 유리하다."],
]
table = Table(
    [[p(cell, styles["SmallKo"]) for cell in row] for row in cap_data],
    colWidths=[34 * mm, 45 * mm, 91 * mm],
)
table.setStyle(TableStyle([
    ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#EEF2FF")),
    ("TEXTCOLOR", (0, 0), (-1, 0), colors.HexColor("#111827")),
    ("GRID", (0, 0), (-1, -1), 0.25, colors.HexColor("#D1D5DB")),
    ("VALIGN", (0, 0), (-1, -1), "TOP"),
    ("LEFTPADDING", (0, 0), (-1, -1), 5),
    ("RIGHTPADDING", (0, 0), (-1, -1), 5),
    ("TOPPADDING", (0, 0), (-1, -1), 5),
    ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
]))
story.append(table)
story.append(Spacer(1, 5 * mm))
story.append(p(
    "논문은 이 능력들이 단독으로 성과를 결정한다고 보지 않는다. 같은 매체라도 사용자가 어떤 규칙으로 사용하느냐, 과제에 얼마나 익숙하냐, "
    "참여자들이 서로를 얼마나 잘 아느냐에 따라 실제 효과는 달라진다.",
    styles["BodyKo"],
))

story.append(PageBreak())
story.append(p("7. 과제와 친숙도의 영향", styles["H1Ko"]))
story.append(p(
    "MST는 같은 과제라도 참여자의 친숙도에 따라 필요한 커뮤니케이션 과정이 달라진다고 본다. "
    "과제와 팀원에 익숙하지 않으면 정보 전달과 해석에 더 많은 시간이 필요하다. 반대로 과제와 팀원에 익숙하면 세부 설명을 줄이고 빠르게 의미를 맞출 수 있다.",
    styles["BodyKo"],
))
story.append(bullet(
    "과제에 익숙하지 않은 경우: 자료 제공, 배경 설명, 근거 검토 같은 conveyance 비중이 커진다.",
    styles["BulletKo"],
))
story.append(bullet(
    "서로를 잘 모르는 경우: 용어, 의도, 맥락을 맞추기 위한 convergence 비중이 커진다.",
    styles["BulletKo"],
))
story.append(bullet(
    "과제와 팀에 모두 익숙한 경우: 짧은 실시간 대화만으로도 의미 조율이 가능해진다.",
    styles["BulletKo"],
))

story.append(p("8. 핵심 명제 요약", styles["H1Ko"]))
story.append(p(
    "논문에는 여러 proposition이 제시되지만, 전체 흐름은 다음처럼 정리할 수 있다.",
    styles["BodyKo"],
))
story.append(bullet(
    "Conveyance 중심 과정에서는 낮은 동시성을 지원하는 매체가 더 적합하다.",
    styles["BulletKo"],
))
story.append(bullet(
    "Convergence 중심 과정에서는 높은 동시성을 지원하는 매체가 더 적합하다.",
    styles["BulletKo"],
))
story.append(bullet(
    "대부분의 협업 과제는 conveyance와 convergence가 모두 필요하다.",
    styles["BulletKo"],
))
story.append(bullet(
    "따라서 하나의 매체만 고집하기보다 과제 단계별로 여러 매체를 조합하는 것이 더 좋은 성과를 낸다.",
    styles["BulletKo"],
))
story.append(bullet(
    "매체의 효과는 기술 속성뿐 아니라 사용자 경험, 팀 규칙, 과제 친숙도에 의해 달라진다.",
    styles["BulletKo"],
))

story.append(p("9. 이 논문이 ‘실시간성’에 주는 의미", styles["H1Ko"]))
story.append(p(
    "이 논문은 ‘실시간성이 언제나 좋다’고 주장하지 않는다. 오히려 실시간성이 좋은 경우와 좋지 않은 경우를 구분한다. "
    "실시간성은 서로의 의미를 맞추고, 빠르게 질문하고, 논점을 조율해야 하는 convergence 과정에 특히 유리하다. "
    "반면 복잡한 근거를 읽고 생각을 정리하는 conveyance 과정에는 비동기적이고 재처리 가능한 매체가 더 적합할 수 있다.",
    styles["BodyKo"],
))
story.append(p(
    "따라서 이 논문을 서비스 기획에 사용할 때는 ‘실시간이라서 무조건 흥미롭다’는 근거로 쓰기보다는, "
    "‘이슈 토론에서 쟁점을 빠르게 맞추고 의미를 조율하기 위해 실시간 상호작용이 필요하다’는 근거로 쓰는 것이 더 정확하다.",
    styles["BodyKo"],
))

story.append(p("10. 우리 서비스에 적용하기", styles["H1Ko"]))
story.append(p(
    "실시간 이슈 기반 토론 서비스에서는 두 가지 커뮤니케이션 과정이 모두 필요하다. "
    "사용자는 먼저 이슈, 근거, 다른 사람의 의견을 읽고 자신의 입장을 정리해야 한다. 이는 conveyance 과정이다. "
    "동시에 토론 중에는 상대 의견에 반응하고, 오해를 풀고, 쟁점을 좁혀야 한다. 이는 convergence 과정이다.",
    styles["BodyKo"],
))
story.append(bullet(
    "실시간 채팅: 참여자들이 즉시 반응하고 분위기와 쟁점을 확인하는 convergence 장치.",
    styles["BulletKo"],
))
story.append(bullet(
    "발언권 기반 메인 스테이지: 동시에 너무 많은 사람이 말하지 않도록 조율하면서, 핵심 발언을 실시간으로 노출하는 convergence 장치.",
    styles["BulletKo"],
))
story.append(bullet(
    "발언 기록, 근거 링크, AI 리포트: 사용자가 내용을 다시 읽고 정리할 수 있게 하는 conveyance 장치.",
    styles["BulletKo"],
))
story.append(bullet(
    "따라서 우리 서비스는 실시간 매체와 기록형 매체를 함께 제공해야 MST 관점에서 더 설득력 있는 설계가 된다.",
    styles["BulletKo"],
))

story.append(p("11. 기획서에 넣기 좋은 문장", styles["H1Ko"]))
story.append(p(
    "Dennis, Fuller, Valacich(2008)의 Media Synchronicity Theory는 커뮤니케이션을 정보 전달 및 처리 과정인 conveyance와 "
    "의미 조율 및 공통 이해 형성 과정인 convergence로 구분한다. 이 이론에 따르면 복잡한 정보를 검토하는 과정에는 낮은 동시성과 재처리 가능성이 중요하고, "
    "참여자들이 서로의 해석을 맞추고 쟁점을 조율하는 과정에는 높은 동시성이 유리하다.",
    styles["BodyKo"],
))
story.append(p(
    "본 서비스는 실시간 이슈에 대해 사용자가 단순히 정보를 읽는 데서 그치지 않고, 다른 참여자의 반응을 확인하며 쟁점을 조율할 수 있도록 "
    "실시간 채팅과 발언권 기반 메인 스테이지를 제공한다. 동시에 근거 링크, 발언 기록, AI 리포트를 통해 사용자가 토론 내용을 다시 검토하고 정리할 수 있도록 보완한다.",
    styles["BodyKo"],
))

story.append(p("12. 발표나 면접에서 설명하는 방식", styles["H1Ko"]))
story.append(p(
    "이 논문을 설명할 때는 다음처럼 말하면 좋다.",
    styles["BodyKo"],
))
story.append(p(
    "“저희가 실시간 토론을 선택한 이유는 단순히 재미있어 보여서가 아니라, 토론에는 서로의 해석을 빠르게 확인하고 쟁점을 맞추는 과정이 필요하기 때문입니다. "
    "Media Synchronicity Theory에 따르면 의미를 조율하는 convergence 과정에는 높은 동시성이 적합합니다. 그래서 채팅과 발언권을 실시간으로 제공했습니다. "
    "다만 근거 탐색과 숙고는 비동기적 처리가 더 적합하므로, 발언 기록과 AI 리포트로 보완했습니다.”",
    styles["BodyKo"],
))

story.append(p("13. 한계와 주의점", styles["H1Ko"]))
story.append(bullet(
    "이 논문은 실시간성이 흥미를 직접 증가시킨다는 논문은 아니다. 실시간성의 역할을 ‘의미 조율과 공통 이해 형성’ 관점에서 설명하는 이론이다.",
    styles["BulletKo"],
))
story.append(bullet(
    "따라서 흥미나 몰입 자체의 근거가 필요하면 온라인 상호작용, 사회적 현존감, 사용자 참여 연구를 추가로 함께 인용하는 것이 좋다.",
    styles["BulletKo"],
))
story.append(bullet(
    "서비스 설계에서는 실시간 채팅만 제공하면 안 되고, 정보 재검토와 기록 확인을 위한 비동기 장치도 함께 제공해야 한다.",
    styles["BulletKo"],
))

story.append(p("14. 최종 정리", styles["H1Ko"]))
story.append(p(
    "이 논문의 가장 중요한 메시지는 ‘매체 선택은 과제와 커뮤니케이션 과정에 맞아야 한다’는 것이다. "
    "실시간 채팅과 발언권은 의미 조율에 강하고, 기록과 리포트는 정보 처리에 강하다. "
    "따라서 우리 서비스가 실시간성과 기록성을 함께 설계하는 것은 MST 관점에서 합리적인 선택이라고 설명할 수 있다.",
    styles["BodyKo"],
))

doc = SimpleDocTemplate(
    OUTPUT,
    pagesize=A4,
    rightMargin=18 * mm,
    leftMargin=18 * mm,
    topMargin=18 * mm,
    bottomMargin=16 * mm,
    title="Media Synchronicity Theory Korean Detailed Summary",
    author="Codex",
)
doc.build(story, onFirstPage=add_page_number, onLaterPages=add_page_number)
print(OUTPUT)
