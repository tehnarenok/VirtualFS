# Virtual File System (VFS)
Данная библиотека предназначена для хранения данных и работы с ними в вуртульной файловой системе, которая сохраняет все данные в одном файле.  
В один файл можно положить несколько виртульных файловых систем

## Работа с VFS

### VirtualFS

### Инизиализация VFS
```java
VirtualFS virtualFS = new VirtualFS(file);
```

```file``` - файл, где будет хранится VFS  
```java
VirtualFS virtualFS = new VirtualFS(file, position);
```
`long position` номер байта, где хранится VFS (для хранения нескльких VFS в одном файле)

### Создание файла
Создание файла в роот папке VFS:
```java
VirtualFile virtualFile = virtualFS.touch(name);
```
Создание файла в директории:
```java
VirtualFile virtualFile = virtualDirectory.touch(name);
```
### Создание директории
Создание директории в роот папке VFS:
```java
VirtualDirectory directory = virtualFS.mkdir(name);
```
Создание директории в директории:
```java
VirtualDirectory directory = virtualDirectory.mkdir(name);
```

### Удаление файла/папки
```java
virtualDirectory.remove();
virtualFile.remove();
```

### Перемещение файла/папки
```java
virtualDirectory.move(destinationDirectory);
virtualFile.move(destinationDirectory);
```


### Переименование файла/папки
```java
virtualDirectory.rename(name);
virtualFile.rename(name);
```

### Копирование файла/папки
```java
virtualDirectory.copy(destinationDirectory);
virtualFile.copy(destinationDirectory);
```

### Поиск в файлов
Для поиска фалов используется метод ```find```, который принимает первым аргументом
* Подстроку, которая должно содержаться в имени файла
* ```Pattern```, для поиска совпадений будет использоваться ```pattern.matcher(fileName)```  
вторым аргуметом является флаг, который использется для рекрсивного поиска, по умолчанию равен ```false```

```java
Iterator<VirtualFile> iterator = new virtualDirectory.find("test", true);
```

### Чтение данных из файла
```java
VirtualRandomAccessFile randomAccessFile = virtualFile.open("r");
String line = randomAccessFile.readLine();
randomAccessFile.close();
```

### Запись данных в файл
```java
VirtualRandomAccessFile randomAccessFile = virtualFile.open("rw");
randomAccessFile.writeLong(123);
randomAccessFile.close();
```

## Работа с VFS в многопоточном режиме
### Правила
#### Если файл открыт на чтение, то:
* Запрещено
  + Премещение любой директории, которая находится по пути от файла до корня VFS
  + Удаление любой директории, которая находится по пути от файла до корня VFS
  + Открытие данного файла на запись
  + Удаление данного файла
  + Перемещение данного файла
* Разрешено
  + Открытие данного файла на чтение
  + Копирование любой директории, которая находится по пути от файла до корня VFS
  + Переименование любой директории, которая находится по пути от файла до корня VFS
  + Добавление файлов в любую директории, которая находится по пути от файла до корня VFS
  + Добавление директорий в любую директории, которая находится по пути от файла до корня VFS



#### Если файл открыт на запись, то:
* Запрещено
  + Премещение любой директории, которая находится по пути от файла до корня VFS
  + Удаление любой директории, которая находится по пути от файла до корня VFS
  + Открытие данного файла на чтение
  + Открытие данного файла на запись
  + Удаление данного файла
  + Перемещение данного файла
* Разрешено
  + Копирование любой директории, которая находится по пути от файла до корня VFS
  + Переименование любой директории, которая находится по пути от файла до корня VFS
  + Добавление файлов в любую директории, которая находится по пути от файла до корня VFS
  + Добавление директорий в любую директории, которая находится по пути от файла до корня VFS
  
При возникновении ошибки при выполнении оперции над файлов системой, связанной с работой другого потока, вернется ошибка 
```java
LockedVirtualFSNode
```
При попытке открыть виртульный файл на запись, уже открытый на запись, вернется ошибка
```java
OverlappingVirtualFileLockException
```
