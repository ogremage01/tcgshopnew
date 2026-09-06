import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter, DialogTrigger, DialogClose } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Checkbox } from "@/components/ui/checkbox";
import { Label } from "@/components/ui/label";
import { StorageDto } from "@/types/product";

export default function StorageEditModal({ handleStorageEdit, storage, open, onOpenChange }: { handleStorageEdit: React.FormEventHandler<HTMLFormElement>, storage: StorageDto, open: boolean, onOpenChange: (open: boolean) => void }) {
    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogTrigger asChild>
                <Button variant="outline">변경</Button>
            </DialogTrigger>
            <DialogContent className="sm:max-w-sm">
                <DialogHeader>
                    <DialogTitle>저장소 변경</DialogTitle>
                    <DialogDescription>
                        저장소 정보를 변경해주세요.
                    </DialogDescription>
                </DialogHeader>
                <form className="flex flex-col gap-2" onSubmit={handleStorageEdit}>
                    <Input name="id" type="hidden" defaultValue={storage.id} />
                    <Input name="storageName" placeholder="저장소 이름" defaultValue={storage.storageName} />
                    <Input name="description" placeholder="저장소 설명" defaultValue={storage.description} />
                    <Label htmlFor="isDefault">기본 저장소 여부 </Label>
                    <Checkbox id="isDefault" name="isDefault" defaultChecked={storage.isDefault} />
                    <DialogFooter>
                        <Button type="submit">변경</Button>
                        <DialogClose asChild>
                            <Button variant="outline" type="button">취소</Button>
                        </DialogClose>
                    </DialogFooter>
                </form>
            </DialogContent>

        </Dialog>
    )
}