import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter, DialogTrigger, DialogClose } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Checkbox } from "@/components/ui/checkbox";
import { Label } from "@/components/ui/label";

export default function StorageAddModal({ handleStorageAdd, open, onOpenChange }: { handleStorageAdd: React.FormEventHandler<HTMLFormElement>, open: boolean, onOpenChange: (open: boolean) => void }) {
    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogTrigger asChild>
                <Button variant="outline">추가</Button>
            </DialogTrigger>
            <DialogContent className="sm:max-w-sm">
                <DialogHeader>
                    <DialogTitle>저장소 추가</DialogTitle>
                    <DialogDescription>
                        저장소 정보를 입력해주세요.
                    </DialogDescription>
                </DialogHeader>
                <form className="flex flex-col gap-2" onSubmit={handleStorageAdd}>
                    <Input name="storageName" placeholder="저장소 이름" />
                    <Input name="description" placeholder="저장소 설명" />
                    <Label htmlFor="isDefault">기본 저장소 여부 </Label>
                    <Checkbox id="isDefault" name="isDefault" />
                    <DialogFooter>
                        <Button type="submit">추가</Button>
                        <DialogClose asChild>
                            <Button variant="outline" type="button">취소</Button>
                        </DialogClose>
                    </DialogFooter>
                </form>
            </DialogContent>

        </Dialog>
    )
}