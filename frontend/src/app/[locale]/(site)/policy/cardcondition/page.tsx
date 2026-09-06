import Image from "next/image";
import { resolveProductImageSrc } from "@/lib/public-asset-url";
export default function CardConditionPage() {
    return (
    <div className="max-w-4xl mx-auto my-3 space-y-8 text-sm leading-7 text-gray-800">
    <section className="space-y-3">
        <h2 className="text-xl font-bold text-gray-950">컨디션 가이드 / Condition Guide</h2>

        <p>
        본점에서 판매하는 카드의 컨디션 평가 기준은 아래와 같습니다.
        <br />
        The condition standards for cards sold by our store are as follows.
        </p>

        <p>
        컨디션 평가는 카드의 개별적인 손상을 종합하여 평가합니다.
        <br />
        Card condition is assessed comprehensively based on each card’s individual signs of damage or wear.
        </p>
    </section>

    <section className="space-y-3">
        <h3 className="text-lg font-bold text-gray-950">
        Near Mint, NM, 니어 민트
        </h3>

        <p>
        손상이 없거나 경미한 경우를 포함합니다.
        <br />
        Includes cards with no damage or only minor signs of wear.
        </p>

        <p>
        약간의 마모, 가벼운 긁힘이 있는 카드가 이 등급에 속할 수 있습니다.
        <br />
        Cards with slight wear or light scratches may fall under this grade.
        </p>
        <div className="flex flex-row gap-2">
            <Image src={resolveProductImageSrc("/uploads/condition-guide/NM1.jpg")} alt="NM" width={300} height={300} />
            <Image src={resolveProductImageSrc("/uploads/condition-guide/NM2.jpg")} alt="NM" width={300} height={300} />
        </div>
    </section>

    <section className="space-y-3">
        <h3 className="text-lg font-bold text-gray-950">
        Excellent, EX, 엑설런트
        </h3>

        <p>
        가벼운 사용감이 있거나 약간의 마모를 가진 경우를 포함합니다.
        <br />
        Includes cards with light signs of use or slight wear.
        </p>

        <p>
        모서리와 테두리의 백화, 다수의 긁힘을 포함한 표면 손상, 약간의 얼룩이나 오염을 포함한 카드가 이에 속할 수 있습니다.
        <br />
        Cards with whitening on corners or edges, surface damage including multiple scratches, or slight stains or dirt may fall under this grade.
        </p>
        <div className="flex flex-row gap-2">
            <Image src={resolveProductImageSrc("/uploads/condition-guide/EX1.jpg")} alt="EX" width={300} height={300} />
            <Image src={resolveProductImageSrc("/uploads/condition-guide/EX2.jpg")} alt="EX" width={300} height={300} />
        </div>
    </section>

    <section className="space-y-3">
        <h3 className="text-lg font-bold text-gray-950">
        Very Good, VG, 베리 굿
        </h3>

        <p>
        상당한 사용감이 있거나 많은 마모를 가진 경우를 포함합니다.
        <br />
        Includes cards with noticeable signs of use or heavy wear.
        </p>

        <p>
        모서리와 테두리의 많은 백화, 눈에 띄는 표면 손상, 상당한 얼룩과 오염, 눌림을 포함한 영구적인 굴곡 변형을 가진 카드가 이에 속할 수 있습니다.
        <br />
        Cards with significant whitening on corners or edges, noticeable surface damage, considerable stains or dirt, or permanent bending deformation including dents may fall under this grade.
        </p>
        <div className="flex flex-row gap-2">
            <Image src={resolveProductImageSrc("/uploads/condition-guide/VG1.jpg")} alt="VG" width={300} height={300} />
            <Image src={resolveProductImageSrc("/uploads/condition-guide/VG2.jpg")} alt="VG" width={300} height={300} />
        </div>
    </section>

    <section className="space-y-3">
        <h3 className="text-lg font-bold text-gray-950">
        Good, G, 굿
        </h3>

        <p>
        토너먼트 게임에 사용할 수 있는 최저의 상태를 포함합니다.
        <br />
        Includes the lowest condition considered usable for tournament play.
        </p>

        <p>
        모든 모서리와 테두리 및 표면의 많은 손상, 상당한 얼룩과 오염, 도장 또는 사인, 광범위한 변형을 가진 카드가 이에 속할 수 있습니다.
        <br />
        Cards with heavy damage across corners, edges, and surfaces, considerable stains or dirt, stamps or signatures, or extensive deformation may fall under this grade.
        </p>
        <div className="flex flex-row gap-2">
            <Image src={resolveProductImageSrc("/uploads/condition-guide/Good1.jpg")} alt="Good1" width={300} height={300} />
            <Image src={resolveProductImageSrc("/uploads/condition-guide/Good2.jpg")} alt="Good2" width={300} height={300} />
        </div>
    </section>
    </div>
);
}