import {
  Select,
  SelectTrigger,
  SelectValue,
  SelectContent,
  SelectItem,
  SelectGroup,
  SelectLabel,
} from "@/components/ui/select";
import {
  Combobox,
  ComboboxContent,
  ComboboxEmpty,
  ComboboxInput,
  ComboboxItem,
  ComboboxList,
} from "@/components/ui/combobox";
import { Button } from "@/components/ui/button";
import { apiClient } from "@/lib/api";
import { useEffect, useMemo, useState } from "react";
import { StorageDto } from "@/types/product";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { GAME_ENUM } from "@/config/gameEnum";
import {
  fetchSetListForGame,
  gameEntryByProductId,
  type GameSetInfoDto,
} from "@/lib/game-set-list";
import { useCardProductLanguages } from "@/hooks/use-card-product-languages";

type SetOption = {
  value: string;
  label: string;
};

function displaySetName(row: GameSetInfoDto | undefined) {
  if (!row) return "";
  return "name" in row ? row.name : row.setName;
}

export default function ProductUploadFileSelect({
  storages,
}: {
  storages: StorageDto[];
}) {
  const [selectedGame, setSelectedGame] = useState<string>("");
  const [sets, setSets] = useState<GameSetInfoDto[]>([]);
  const [selectedSetCode, setSelectedSetCode] = useState<string>("");
  const [language, setLanguage] = useState<string>("en");
  const [storageId, setStorageId] = useState<string>("1");
  const [condition, setCondition] = useState<string>("NM");
  const [printType, setPrintType] = useState<string>("Normal");
  const [isVisible, setIsVisible] = useState<string>("true");
  const { languages } = useCardProductLanguages();
  const games = GAME_ENUM.map((g) => ({
    productLineId: g.productId,
    productLineName: g.game,
  }));

  //세트 목록 조회
  useEffect(() => {
    if (!selectedGame) {
      return;
    }

    const entry = gameEntryByProductId(selectedGame);
    if (!entry) {
      setSets([]);
      return;
    }

    void fetchSetListForGame(entry).then((response) => {
      const normalizedSets = response.filter((row) => Boolean(row?.setCode));
      setSets(normalizedSets);
    });
  }, [selectedGame]);

  useEffect(() => {
    if (!storages.length) return;
    setStorageId((prev) =>
      storages.some((s) => String(s.id) === prev)
        ? prev
        : String(storages[0].id),
    );
  }, [storages]);

  const [maxVisibleStock, setMaxVisibleStock] = useState<string>("8");

  const setOptions = useMemo<SetOption[]>(() => {
    return sets.map((row) => {
      const name = displaySetName(row);
      return {
        value: row.setCode,
        label: `${row.setCode} - ${name}`,
      };
    });
  }, [sets]);

  const selectedSetOption = useMemo(() => {
    return (
      setOptions.find((option) => option.value === selectedSetCode) ?? null
    );
  }, [selectedSetCode, setOptions]);

  const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const formData = new FormData(e.target as HTMLFormElement);
    console.log("[ProductUploadFileSelect] formData", formData);
    const selectedGameData = games.find(
      (item) => String(item.productLineId) === selectedGame,
    );
    const gameName = selectedGameData?.productLineName;
    const selectedSetMeta = sets.find((s) => s.setCode === selectedSetCode);
    const maxStockNum = Number(maxVisibleStock);
    const submitState = {
      selectedGame,
      gameName,
      selectedSetCode,
      storageId,
      language,
      condition,
      printType,
      isVisible,
      maxVisibleStock,
      setsCount: sets.length,
    };
    console.log("[ProductUploadFileSelect] submit state", submitState);

    if (!selectedGame.trim()) {
      alert("게임을 선택해주세요.");
      return;
    }
    if (!gameName) {
      alert("선택한 게임 정보를 찾을 수 없습니다.");
      return;
    }
    if (!selectedSetCode.trim()) {
      alert("세트를 선택해주세요.");
      return;
    }
    if (
      !storageId.trim() ||
      !storages.some((s) => String(s.id) === storageId)
    ) {
      alert("저장소를 선택해주세요.");
      return;
    }
    if (!Number.isFinite(maxStockNum) || maxStockNum < 0) {
      alert("최대 표시 수량을 올바르게 입력해주세요.");
      return;
    }

    const selectedSetName = displaySetName(selectedSetMeta);

    const payload = {
      gameName: gameName,
      setName: selectedSetName ?? "",
      setCode: selectedSetMeta?.setCode ?? selectedSetCode,
      language: language,
      storageId: Number(storageId),
      isVisible: isVisible === "true" ? true : false,
      condition: condition,
      maxVisibleStock: maxStockNum,
      printType: printType,
    };
    console.log("[ProductUploadFileSelect] request payload", payload);

    return apiClient
      .post(`/api/admin/product/single-products/multiple/download`, payload, {
        responseType: "blob",
      })
      .then((response) => {
        const blob = new Blob([response.data], {
          type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        });
        const contentDisposition = response.headers["content-disposition"] as
          | string
          | undefined;
        const fileNameMatch = contentDisposition?.match(/filename="?([^"]+)"?/);
        const fileName = fileNameMatch?.[1] ?? "products.xlsx";

        const downloadUrl = URL.createObjectURL(blob);
        const anchor = document.createElement("a");
        anchor.href = downloadUrl;
        anchor.download = fileName;
        document.body.appendChild(anchor);
        anchor.click();
        anchor.remove();
        URL.revokeObjectURL(downloadUrl);
      })
      .catch((error) => {
        console.error("[ProductUploadFileSelect] download failed", {
          status: error?.response?.status,
          data: error?.response?.data,
          message: error?.message,
        });
      });
  };

  return (
    <form className="flex flex-col gap-2 items-end" onSubmit={handleSubmit}>
      <Select
        // required
        value={selectedGame}
        onValueChange={(value) => {
          setSelectedGame(value);
          setSets([]);
          setSelectedSetCode("");
        }}
      >
        <SelectTrigger>
          <SelectValue placeholder="게임을 선택해주세요." />
        </SelectTrigger>
        <SelectContent className="bg-white">
          <SelectGroup>
            <SelectLabel>게임 이름</SelectLabel>
            {games?.map((game) => (
              <SelectItem
                key={game.productLineName}
                value={String(game.productLineId)}
              >
                {game.productLineName}
              </SelectItem>
            ))}
          </SelectGroup>
        </SelectContent>
      </Select>

      <Combobox
        items={setOptions}
        value={selectedSetOption}
        onValueChange={(option) => setSelectedSetCode(option?.value ?? "")}
        disabled={!selectedGame}
        isItemEqualToValue={(a, b) => a.value === b.value}
      >
        <ComboboxInput
          className="w-full"
          placeholder="세트를 검색하거나 선택해주세요."
          disabled={!selectedGame}
        />
        <ComboboxContent className="bg-white p-0">
          <ComboboxEmpty>검색 결과가 없습니다.</ComboboxEmpty>
          <ComboboxList className="max-h-60 overflow-y-auto">
            {(item) => (
              <ComboboxItem key={item.value} value={item}>
                {item.label}
              </ComboboxItem>
            )}
          </ComboboxList>
        </ComboboxContent>
      </Combobox>
      <Select value={language} onValueChange={setLanguage}>
        <SelectTrigger>
          <SelectValue placeholder="언어를 선택해주세요." />
        </SelectTrigger>
        <SelectContent className="bg-white">
          <SelectGroup>
            <SelectLabel>언어</SelectLabel>
            {languages.map((item) => (
              <SelectItem key={item.code} value={item.code}>
                {item.displayName}
              </SelectItem>
            ))}
          </SelectGroup>
        </SelectContent>
      </Select>
      <Select value={storageId} onValueChange={setStorageId}>
        <SelectTrigger>
          <SelectValue placeholder="저장소를 선택해주세요." />
        </SelectTrigger>
        <SelectContent className="bg-white">
          <SelectGroup>
            <SelectLabel>저장소</SelectLabel>
            {storages?.map((storage) => (
              <SelectItem key={storage.id} value={storage.id.toString()}>
                {storage.storageName}
              </SelectItem>
            ))}
          </SelectGroup>
        </SelectContent>
      </Select>
      <Select value={condition} onValueChange={setCondition}>
        <SelectTrigger>
          <SelectValue placeholder="카드 상태를 선택해주세요." />
        </SelectTrigger>
        <SelectContent className="bg-white">
          <SelectGroup>
            <SelectLabel>카드 상태</SelectLabel>
            <SelectItem value="NM">NM: Near Mint</SelectItem>
            <SelectItem value="EX">EX: Excellent</SelectItem>
            <SelectItem value="VG">VG: Very Good</SelectItem>
            <SelectItem value="G">G: Good</SelectItem>
          </SelectGroup>
        </SelectContent>
      </Select>
      <div className="flex flex-row gap-2 items-center justify-between w-full">
        <Label className="w-36" htmlFor="maxVisibleStock">
          타입
        </Label>
        <Select value={printType} onValueChange={setPrintType}>
          <SelectTrigger>
            <SelectValue placeholder="일반/포일 중 선택해주세요." />
          </SelectTrigger>
          <SelectContent className="bg-white">
            <SelectGroup>
              <SelectItem value="foil">포일</SelectItem>
              <SelectItem value="Normal">일반</SelectItem>
            </SelectGroup>
          </SelectContent>
        </Select>
      </div>
      <div className="flex flex-row gap-2 items-center justify-between w-full">
        <Label className="w-36" htmlFor="maxVisibleStock">
          최대 표시 수량
        </Label>
        <Input
          id="maxVisibleStock"
          type="number"
          min={0}
          placeholder="최대 표시 수량을 입력해주세요."
          value={maxVisibleStock}
          onChange={(ev) => setMaxVisibleStock(ev.target.value)}
        />
      </div>
      <div className="flex flex-row gap-2 items-center justify-between w-full">
        <Label className="w-36" htmlFor="isVisible">
          공개 여부
        </Label>
        <Select value={isVisible} onValueChange={setIsVisible}>
          <SelectTrigger>
            <SelectValue placeholder="공개 여부를 선택해주세요." />
          </SelectTrigger>
          <SelectContent className="bg-white">
            <SelectGroup>
              <SelectItem value="true">공개</SelectItem>
              <SelectItem value="false">비공개</SelectItem>
            </SelectGroup>
          </SelectContent>
        </Select>
      </div>
      <Button className="w-fit" type="submit">
        파일 다운로드
      </Button>
    </form>
  );
}
