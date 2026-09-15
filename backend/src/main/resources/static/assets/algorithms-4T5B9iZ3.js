const n=[{key:"sorting",name:"排序算法",icon:"🔢",desc:"将数据元素按指定顺序重新排列的算法"},{key:"searching",name:"查找算法",icon:"🔍",desc:"在数据集合中定位目标元素的算法"},{key:"tree",name:"树结构",icon:"🌳",desc:"二叉树、BST、AVL、哈夫曼等树形结构算法"},{key:"graph",name:"图算法",icon:"🕸️",desc:"图的遍历、最短路径、最小生成树、拓扑排序"},{key:"linear",name:"线性结构",icon:"📚",desc:"栈、队列、链表等线性数据结构"}],r=[{key:"bubble",category:"sorting",name:"冒泡排序",desc:"相邻元素两两比较，逐步把最大元素“冒泡”到序列末尾。",component:"SortVisualizer",difficulty:"简单",time:"O(n²)",space:"O(1)",stable:"稳定",steps:["从序列头部开始，依次比较相邻两个元素","若顺序错误则交换它们","每一轮将当前未排序区间的最大值移动到右端","重复上述过程直到整个序列有序"],pseudo:`for i = 0 to n-1:
    for j = 0 to n-i-2:
        if a[j] > a[j+1]:
            swap(a[j], a[j+1])`,code:`void bubbleSort(int arr[], int n) {
    for (int i = 0; i < n-1; i++) {
        for (int j = 0; j < n-i-1; j++) {
            if (arr[j] > arr[j+1]) {
                int temp = arr[j];
                arr[j] = arr[j+1];
                arr[j+1] = temp;
            }
        }
    }
}`},{key:"selection",category:"sorting",name:"选择排序",desc:"每次从剩余元素中选择最小值，放到已排序区的末尾。",component:"SortVisualizer",difficulty:"简单",time:"O(n²)",space:"O(1)",stable:"不稳定",steps:["将序列分为已排序区和未排序区","在未排序区中查找最小元素","将最小元素与未排序区首元素交换","扩大已排序区，重复直到全部有序"],pseudo:`for i = 0 to n-1:
    min = i
    for j = i+1 to n-1:
        if a[j] < a[min]:
            min = j
    swap(a[i], a[min])`,code:`void selectionSort(int arr[], int n) {
    for (int i = 0; i < n-1; i++) {
        int minIdx = i;
        for (int j = i+1; j < n; j++) {
            if (arr[j] < arr[minIdx]) {
                minIdx = j;
            }
        }
        int temp = arr[i];
        arr[i] = arr[minIdx];
        arr[minIdx] = temp;
    }
}`},{key:"insertion",category:"sorting",name:"直接插入排序",desc:"逐个将未排序元素插入到已排序序列的合适位置。",component:"SortVisualizer",difficulty:"简单",time:"O(n²)",space:"O(1)",stable:"稳定",steps:["认为第一个元素已经有序","取出下一个元素，在已排序序列中从后向前扫描","找到合适位置后插入","重复直到所有元素插入完成"],pseudo:`for i = 1 to n-1:
    key = a[i]
    j = i - 1
    while j >= 0 and a[j] > key:
        a[j+1] = a[j]
        j--
    a[j+1] = key`,code:`void insertionSort(int arr[], int n) {
    for (int i = 1; i < n; i++) {
        int key = arr[i];
        int j = i - 1;
        while (j >= 0 && arr[j] > key) {
            arr[j+1] = arr[j];
            j--;
        }
        arr[j+1] = key;
    }
}`},{key:"shell",category:"sorting",name:"希尔排序",desc:"按一定增量分组进行插入排序，逐步缩小增量至 1。",component:"SortVisualizer",difficulty:"中等",time:"O(n^1.3) ~ O(n²)",space:"O(1)",stable:"不稳定",steps:["选择一个初始增量 gap","将相距 gap 的元素分为一组进行插入排序","缩小 gap 并重复分组排序","当 gap = 1 时执行一次完整的插入排序"],pseudo:`gap = n // 2
while gap > 0:
    for i = gap to n-1:
        key = a[i]
        j = i
        while j >= gap and a[j-gap] > key:
            a[j] = a[j-gap]
            j -= gap
        a[j] = key
    gap //= 2`,code:`void shellSort(int arr[], int n) {
    for (int gap = n/2; gap > 0; gap /= 2) {
        for (int i = gap; i < n; i++) {
            int key = arr[i];
            int j = i;
            while (j >= gap && arr[j-gap] > key) {
                arr[j] = arr[j-gap];
                j -= gap;
            }
            arr[j] = key;
        }
    }
}`},{key:"merge",category:"sorting",name:"归并排序",desc:"分治法将序列二分、排序，再合并两个有序子序列。",component:"SortVisualizer",difficulty:"中等",time:"O(n log n)",space:"O(n)",stable:"稳定",steps:["将当前区间从中间一分为二","递归地对左右两个子区间排序","合并两个已排序子区间","合并时依次取较小元素放入临时数组"],pseudo:`mergeSort(a, l, r):
    if l >= r: return
    m = (l + r) / 2
    mergeSort(a, l, m)
    mergeSort(a, m+1, r)
    merge(a, l, m, r)`,code:`void merge(int arr[], int l, int m, int r) {
    int n1 = m - l + 1, n2 = r - m;
    int L[n1], R[n2];
    for (int i = 0; i < n1; i++) L[i] = arr[l+i];
    for (int j = 0; j < n2; j++) R[j] = arr[m+1+j];
    int i = 0, j = 0, k = l;
    while (i < n1 && j < n2) {
        arr[k++] = (L[i] <= R[j]) ? L[i++] : R[j++];
    }
    while (i < n1) arr[k++] = L[i++];
    while (j < n2) arr[k++] = R[j++];
}

void mergeSort(int arr[], int l, int r) {
    if (l < r) {
        int m = l + (r - l) / 2;
        mergeSort(arr, l, m);
        mergeSort(arr, m+1, r);
        merge(arr, l, m, r);
    }
}`},{key:"quick",category:"sorting",name:"快速排序",desc:"选取枢轴将序列分区，再递归排序左右子序列。",component:"SortVisualizer",difficulty:"中等",time:"O(n log n)",space:"O(log n)",stable:"不稳定",steps:["选择一个枢轴元素（通常选末尾）","将小于枢轴的元素移到左侧，大于的移到右侧","枢轴到达最终位置","递归处理左右两个子区间"],pseudo:`quickSort(a, l, r):
    if l < r:
        p = partition(a, l, r)
        quickSort(a, l, p-1)
        quickSort(a, p+1, r)`,code:`int partition(int arr[], int low, int high) {
    int pivot = arr[high];
    int i = low - 1;
    for (int j = low; j < high; j++) {
        if (arr[j] <= pivot) {
            i++;
            int temp = arr[i];
            arr[i] = arr[j];
            arr[j] = temp;
        }
    }
    int temp = arr[i+1];
    arr[i+1] = arr[high];
    arr[high] = temp;
    return i + 1;
}

void quickSort(int arr[], int low, int high) {
    if (low < high) {
        int pi = partition(arr, low, high);
        quickSort(arr, low, pi - 1);
        quickSort(arr, pi + 1, high);
    }
}`},{key:"heap",category:"sorting",name:"堆排序",desc:"利用大顶堆性质，每次将堆顶元素与末尾交换并调整堆。",component:"SortVisualizer",difficulty:"较难",time:"O(n log n)",space:"O(1)",stable:"不稳定",steps:["将无序序列构建成大顶堆","将堆顶元素（最大值）与末尾元素交换","排除末尾已排序元素，重新调整堆","重复直到所有元素有序"],pseudo:`buildMaxHeap(a)
for i = n-1 down to 1:
    swap(a[0], a[i])
    heapify(a, 0, i)`,code:`void heapify(int arr[], int n, int i) {
    int largest = i;
    int left = 2*i + 1;
    int right = 2*i + 2;
    if (left < n && arr[left] > arr[largest])
        largest = left;
    if (right < n && arr[right] > arr[largest])
        largest = right;
    if (largest != i) {
        int temp = arr[i];
        arr[i] = arr[largest];
        arr[largest] = temp;
        heapify(arr, n, largest);
    }
}

void heapSort(int arr[], int n) {
    for (int i = n/2 - 1; i >= 0; i--)
        heapify(arr, n, i);
    for (int i = n-1; i > 0; i--) {
        int temp = arr[0];
        arr[0] = arr[i];
        arr[i] = temp;
        heapify(arr, i, 0);
    }
}`},{key:"sequential",category:"searching",name:"顺序查找",desc:"从序列一端开始逐个比较，直到找到目标或遍历结束。",component:"SearchVisualizer",difficulty:"简单",time:"O(n)",space:"O(1)",stable:"-",steps:["从第一个元素开始扫描","逐个与目标关键字比较","若相等则查找成功","若扫描完仍未找到则失败"],pseudo:`for i = 0 to n-1:
    if a[i] == key:
        return i
return -1`,code:`int sequentialSearch(int arr[], int n, int key) {
    for (int i = 0; i < n; i++) {
        if (arr[i] == key) {
            return i;
        }
    }
    return -1;
}`},{key:"binary",category:"searching",name:"二分查找",desc:"在有序序列中反复折半，缩小目标所在区间。",component:"SearchVisualizer",difficulty:"简单",time:"O(log n)",space:"O(1)",stable:"-",steps:["确定查找区间的左右边界","取中间元素与目标比较","若目标较小，则在左半区继续查找","若目标较大，则在右半区继续查找","重复直到找到或区间为空"],pseudo:`while low <= high:
    mid = (low + high) / 2
    if a[mid] == key: return mid
    else if a[mid] < key: low = mid + 1
    else: high = mid - 1
return -1`,code:`int binarySearch(int arr[], int low, int high, int key) {
    while (low <= high) {
        int mid = low + (high - low) / 2;
        if (arr[mid] == key)
            return mid;
        if (arr[mid] < key)
            low = mid + 1;
        else
            high = mid - 1;
    }
    return -1;
}`},{key:"bst-search",category:"searching",name:"二叉排序树查找",desc:"利用 BST 左小右大的性质，从根节点向下定位目标。",component:"TreeVisualizer",difficulty:"中等",time:"O(h)",space:"O(1)",stable:"-",steps:["从根节点开始","若目标值等于当前节点，查找成功","若目标值小于当前节点，转向左子树","若目标值大于当前节点，转向右子树"],pseudo:`node = root
while node != null:
    if key == node.val: return node
    if key < node.val: node = node.left
    else: node = node.right
return null`,code:`typedef struct Node {
    int val;
    struct Node *left, *right;
} Node;

Node* bstSearch(Node* root, int key) {
    while (root != NULL) {
        if (key == root->val) return root;
        if (key < root->val) root = root->left;
        else root = root->right;
    }
    return NULL;
}`},{key:"hash",category:"searching",name:"散列表查找",desc:"通过哈希函数计算地址，结合冲突处理方法定位记录。",component:"SearchVisualizer",difficulty:"中等",time:"O(1) 平均",space:"O(n)",stable:"-",steps:["根据关键字计算哈希地址","访问对应哈希表位置","若发生冲突，按开放定址或链地址法处理","找到匹配关键字或确定不存在"],pseudo:`addr = hash(key)
while table[addr] != null:
    if table[addr].key == key: return addr
    addr = next(addr) // 冲突处理
return -1`,code:`#define SIZE 100
int hashTable[SIZE];

int hash(int key) {
    return key % SIZE;
}

int hashSearch(int key) {
    int addr = hash(key);
    while (hashTable[addr] != 0) {
        if (hashTable[addr] == key)
            return addr;
        addr = (addr + 1) % SIZE;
    }
    return -1;
}`},{key:"kmp",category:"searching",name:"KMP 字符串匹配",desc:"利用前缀函数 next[] 使主串指针不回溯，失配时模式串滑动到合适位置。",component:"KmpVisualizer",difficulty:"较难",time:"O(n+m)",space:"O(m)",stable:"-",steps:["先对模式串求前缀函数 next[]（最长相等前后缀长度）","主串指针 i 与模式串指针 j 同步扫描","字符相等时 i、j 同时后移","失配且 j>0 时，j 回溯为 next[j-1]，i 不动","失配且 j=0 时，i 后移","当 j == m 时在主串找到匹配位置"],pseudo:`// 构造 next[](使用 0 起始的下标)
for i = 1 to m-1:
    j = next[i-1]
    while j > 0 and p[i] != p[j]: j = next[j-1]
    next[i] = p[i] == p[j] ? j+1 : 0
// 匹配阶段
i = 0, j = 0
while i < n:
    if t[i] == p[j]: i++; j++
    elif j > 0: j = next[j-1]      // i 不回溯
    else: i++
    if j == m: 位置 i-m 匹配`,code:`// 构造 next[]：0 起始下标
void buildNext(char p[], int m, int next[]) {
    next[0] = 0;
    int j = 0;
    for (int i = 1; i < m; i++) {
        while (j > 0 && p[i] != p[j]) j = next[j-1];
        if (p[i] == p[j]) j++;
        next[i] = j;
    }
}
int kmp(char t[], int n, char p[], int m, int next[]) {
    int i = 0, j = 0;
    while (i < n && j < m) {
        if (t[i] == p[j]) { i++; j++; }
        else if (j > 0) j = next[j-1];  // i 不回溯
        else i++;
        if (j == m) return i - m;        // 匹配成功
    }
    return -1;
}`},{key:"bst-traversal",category:"tree",name:"二叉树遍历",desc:"按前序、中序、后序或层次顺序访问二叉树所有节点。",component:"TreeVisualizer",difficulty:"简单",time:"O(n)",space:"O(h)",stable:"-",steps:["选择遍历方式","前序：根 → 左 → 右","中序：左 → 根 → 右","后序：左 → 右 → 根","层次：借助队列逐层访问"],pseudo:`inOrder(node):
    if node == null: return
    inOrder(node.left)
    visit(node)
    inOrder(node.right)`,code:`typedef struct Node {
    int val;
    struct Node *left, *right;
} Node;

void inOrder(Node* node) {
    if (node == NULL) return;
    inOrder(node->left);
    printf("%d ", node->val);
    inOrder(node->right);
}`},{key:"bst",category:"tree",name:"二叉排序树",desc:"BST 的插入、删除与查找过程，保持左小右大特性。",component:"TreeVisualizer",difficulty:"中等",time:"O(h)",space:"O(1)",stable:"-",steps:["从根节点开始比较","小于当前节点则进入左子树","大于当前节点则进入右子树","到达空位置时插入新节点"],pseudo:`insert(root, val):
    if root == null: return new Node(val)
    if val < root.val: root.left = insert(root.left, val)
    else if val > root.val: root.right = insert(root.right, val)
    return root`,code:`typedef struct Node {
    int val;
    struct Node *left, *right;
} Node;

Node* bstInsert(Node* root, int val) {
    if (root == NULL) {
        root = (Node*)malloc(sizeof(Node));
        root->val = val;
        root->left = root->right = NULL;
        return root;
    }
    if (val < root->val)
        root->left = bstInsert(root->left, val);
    else if (val > root->val)
        root->right = bstInsert(root->right, val);
    return root;
}`},{key:"avl",category:"tree",name:"平衡二叉树 AVL",desc:"插入节点后若失衡，通过 LL、RR、LR、RL 旋转恢复平衡。",component:"TreeVisualizer",difficulty:"较难",time:"O(log n)",space:"O(1)",stable:"-",steps:["按 BST 规则插入新节点","从插入点向上检查平衡因子","若某节点平衡因子绝对值大于 1，则进行旋转","根据失衡类型选择 LL、RR、LR 或 RL 旋转"],pseudo:`insert(node, val):
    node = bstInsert(node, val)
    balance = height(node.left) - height(node.right)
    if balance > 1 and val < node.left.val:
        return rightRotate(node)
    // 其他旋转情况类似
    return node`,code:`typedef struct Node {
    int val, height;
    struct Node *left, *right;
} Node;

int height(Node* n) {
    return n == NULL ? 0 : n->height;
}

Node* rightRotate(Node* y) {
    Node* x = y->left;
    Node* T2 = x->right;
    x->right = y;
    y->left = T2;
    y->height = max(height(y->left), height(y->right))+1;
    x->height = max(height(x->left), height(x->right))+1;
    return x;
}

Node* leftRotate(Node* x) {
    Node* y = x->right;
    Node* T2 = y->left;
    y->left = x;
    x->right = T2;
    x->height = max(height(x->left), height(x->right))+1;
    y->height = max(height(y->left), height(y->right))+1;
    return y;
}

Node* avlInsert(Node* node, int val) {
    if (node == NULL) {
        node = (Node*)malloc(sizeof(Node));
        node->val = val;
        node->left = node->right = NULL;
        node->height = 1;
        return node;
    }
    if (val < node->val)
        node->left = avlInsert(node->left, val);
    else if (val > node->val)
        node->right = avlInsert(node->right, val);
    else return node;
    
    node->height = 1 + max(height(node->left), height(node->right));
    int balance = height(node->left) - height(node->right);
    
    if (balance > 1 && val < node->left->val)
        return rightRotate(node);
    if (balance < -1 && val > node->right->val)
        return leftRotate(node);
    if (balance > 1 && val > node->left->val) {
        node->left = leftRotate(node->left);
        return rightRotate(node);
    }
    if (balance < -1 && val < node->right->val) {
        node->right = rightRotate(node->right);
        return leftRotate(node);
    }
    return node;
}`},{key:"huffman",category:"tree",name:"哈夫曼树",desc:"贪心构造带权路径长度最小的二叉树，常用于数据压缩。",component:"TreeVisualizer",difficulty:"中等",time:"O(n log n)",space:"O(n)",stable:"-",steps:["将每个权值作为独立节点放入优先队列","每次取出权值最小的两个节点","合并为新节点，权值为两者之和，重新入队","重复直到只剩一个根节点"],pseudo:`while queue.size() > 1:
    a = queue.popMin()
    b = queue.popMin()
    parent = new Node(a.w + b.w)
    parent.left = a; parent.right = b
    queue.push(parent)
return queue.popMin()`,code:`typedef struct Node {
    int weight;
    struct Node *left, *right;
} Node;

Node* createNode(int w) {
    Node* node = (Node*)malloc(sizeof(Node));
    node->weight = w;
    node->left = node->right = NULL;
    return node;
}

Node* buildHuffman(int weights[], int n) {
    Node** nodes = (Node**)malloc(n * sizeof(Node*));
    for (int i = 0; i < n; i++)
        nodes[i] = createNode(weights[i]);
    
    for (int i = 1; i < n; i++) {
        int min1 = -1, min2 = -1;
        for (int j = 0; j < n; j++) {
            if (nodes[j] && (min1 == -1 || nodes[j]->weight < nodes[min1]->weight)) {
                min2 = min1; min1 = j;
            } else if (nodes[j] && (min2 == -1 || nodes[j]->weight < nodes[min2]->weight)) {
                min2 = j;
            }
        }
        Node* parent = createNode(nodes[min1]->weight + nodes[min2]->weight);
        parent->left = nodes[min1];
        parent->right = nodes[min2];
        nodes[min1] = parent;
        nodes[min2] = NULL;
    }
    return nodes[0];
}`},{key:"dfs",category:"graph",name:"深度优先搜索 DFS",desc:"沿着一条路径尽可能深地访问顶点，然后回溯。",component:"GraphVisualizer",difficulty:"简单",time:"O(V+E)",space:"O(V)",stable:"-",steps:["从起始顶点开始并标记为已访问","选择一条未访问的邻接边深入","到达死胡同时回溯","重复直到所有可达顶点被访问"],pseudo:`DFS(v):
    visit(v); mark v
    for each neighbor u of v:
        if u not marked: DFS(u)`,code:`#define MAX 100
int visited[MAX];
int adj[MAX][MAX];

void DFS(int v, int n) {
    printf("%d ", v);
    visited[v] = 1;
    for (int i = 0; i < n; i++) {
        if (adj[v][i] == 1 && !visited[i]) {
            DFS(i, n);
        }
    }
}`},{key:"bfs",category:"graph",name:"广度优先搜索 BFS",desc:"按层次扩散，先访问所有邻接顶点，再依次访问下一层。",component:"GraphVisualizer",difficulty:"简单",time:"O(V+E)",space:"O(V)",stable:"-",steps:["起始顶点入队并标记","队头顶点出队并访问","将其所有未访问邻接顶点入队","重复直到队列为空"],pseudo:`queue.push(start); mark start
while queue not empty:
    v = queue.pop()
    visit(v)
    for each neighbor u of v:
        if u not marked:
            mark u; queue.push(u)`,code:`#define MAX 100
int visited[MAX];
int adj[MAX][MAX];

void BFS(int start, int n) {
    int queue[MAX], front = 0, rear = 0;
    visited[start] = 1;
    queue[rear++] = start;
    
    while (front < rear) {
        int v = queue[front++];
        printf("%d ", v);
        for (int i = 0; i < n; i++) {
            if (adj[v][i] == 1 && !visited[i]) {
                visited[i] = 1;
                queue[rear++] = i;
            }
        }
    }
}`},{key:"dijkstra",category:"graph",name:"Dijkstra 最短路径",desc:"求解单源非负权图中到其他各顶点的最短距离。",component:"GraphVisualizer",difficulty:"中等",time:"O((V+E) log V)",space:"O(V)",stable:"-",steps:["初始化源点距离为 0，其余为无穷大","每次选择距离最小的未确定顶点","用该顶点松弛其邻接边","重复直到所有顶点距离确定"],pseudo:`dist[s] = 0; dist[others] = ∞
while 未确定集合非空:
    u = dist 最小的顶点
    for each v adjacent to u:
        if dist[u] + w(u,v) < dist[v]:
            dist[v] = dist[u] + w(u,v)`,code:`#define MAX 100
#define INF 99999

void dijkstra(int graph[MAX][MAX], int n, int src) {
    int dist[MAX], visited[MAX];
    for (int i = 0; i < n; i++) {
        dist[i] = INF;
        visited[i] = 0;
    }
    dist[src] = 0;
    
    for (int i = 0; i < n-1; i++) {
        int u = -1, min = INF;
        for (int j = 0; j < n; j++) {
            if (!visited[j] && dist[j] < min) {
                min = dist[j];
                u = j;
            }
        }
        visited[u] = 1;
        
        for (int v = 0; v < n; v++) {
            if (!visited[v] && graph[u][v] && dist[u] != INF &&
                dist[u] + graph[u][v] < dist[v]) {
                dist[v] = dist[u] + graph[u][v];
            }
        }
    }
}`},{key:"prim",category:"graph",name:"Prim 最小生成树",desc:"从某顶点出发，逐步选择连接树与非树顶点的最小边。",component:"GraphVisualizer",difficulty:"中等",time:"O((V+E) log V)",space:"O(V)",stable:"-",steps:["任选一个顶点加入生成树","在所有连接树内与树外顶点的边中选择权值最小者","将该边及外部顶点加入生成树","重复直到所有顶点都在树中"],pseudo:`T = {s}; dist[v] = w(s,v)
while T != V:
    u = V-T 中 dist 最小的顶点
    T.add(u)
    for each v in V-T:
        dist[v] = min(dist[v], w(u,v))`,code:`#define MAX 100
#define INF 99999

void prim(int graph[MAX][MAX], int n) {
    int key[MAX], mstSet[MAX];
    for (int i = 0; i < n; i++) {
        key[i] = INF;
        mstSet[i] = 0;
    }
    key[0] = 0;
    
    for (int i = 0; i < n-1; i++) {
        int u = -1, min = INF;
        for (int j = 0; j < n; j++) {
            if (!mstSet[j] && key[j] < min) {
                min = key[j];
                u = j;
            }
        }
        mstSet[u] = 1;
        
        for (int v = 0; v < n; v++) {
            if (graph[u][v] && !mstSet[v] && graph[u][v] < key[v]) {
                key[v] = graph[u][v];
            }
        }
    }
}`},{key:"kruskal",category:"graph",name:"Kruskal 最小生成树",desc:"按边权升序选边，用并查集判断是否会形成环。",component:"GraphVisualizer",difficulty:"中等",time:"O(E log E)",space:"O(V)",stable:"-",steps:["将所有边按权值从小到大排序","依次考察每条边","若两端点不在同一集合则加入生成树","使用并查集合并两个集合"],pseudo:`sort edges by weight
for each edge (u,v) in order:
    if find(u) != find(v):
        add edge to MST
        union(u, v)`,code:`#define MAXE 1000

typedef struct Edge {
    int u, v, weight;
} Edge;

int parent[MAXE];

int find(int x) {
    if (parent[x] != x)
        parent[x] = find(parent[x]);
    return parent[x];
}

void unite(int x, int y) {
    parent[find(x)] = find(y);
}

void kruskal(Edge edges[], int n, int e) {
    for (int i = 0; i < n; i++) parent[i] = i;
    
    for (int i = 0; i < e-1; i++) {
        for (int j = 0; j < e-i-1; j++) {
            if (edges[j].weight > edges[j+1].weight) {
                Edge temp = edges[j];
                edges[j] = edges[j+1];
                edges[j+1] = temp;
            }
        }
    }
    
    for (int i = 0; i < e; i++) {
        int u = edges[i].u, v = edges[i].v;
        if (find(u) != find(v)) {
            unite(u, v);
        }
    }
}`},{key:"floyd",category:"graph",name:"Floyd 全源最短路径",desc:"动态规划求解所有顶点对之间的最短距离。",component:"GraphVisualizer",difficulty:"中等",time:"O(V³)",space:"O(V²)",stable:"-",steps:["初始化距离矩阵为邻接矩阵","依次将每个顶点 k 作为中间点","更新所有 i,j 的距离：dist[i][j] = min(dist[i][j], dist[i][k] + dist[k][j])","三重循环结束后得到全源最短路"],pseudo:`for k = 0 to V-1:
    for i = 0 to V-1:
        for j = 0 to V-1:
            dist[i][j] = min(dist[i][j], dist[i][k]+dist[k][j])`,code:`#define MAX 100
#define INF 99999

void floyd(int dist[MAX][MAX], int n) {
    for (int k = 0; k < n; k++) {
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (dist[i][k] + dist[k][j] < dist[i][j]) {
                    dist[i][j] = dist[i][k] + dist[k][j];
                }
            }
        }
    }
}`},{key:"topological",category:"graph",name:"拓扑排序",desc:"对有向无环图（DAG）的顶点进行线性排序。",component:"GraphVisualizer",difficulty:"中等",time:"O(V+E)",space:"O(V)",stable:"-",steps:["计算每个顶点的入度","将入度为 0 的顶点入队","依次出队并输出，同时将其邻接点入度减 1","若邻接点入度变为 0 则入队"],pseudo:`queue = vertices with in-degree 0
while queue not empty:
    v = queue.pop(); output v
    for each edge v->u:
        in-degree[u]--
        if in-degree[u] == 0: queue.push(u)`,code:`#define MAX 100

void topologicalSort(int adj[MAX][MAX], int n) {
    int inDegree[MAX] = {0};
    for (int i = 0; i < n; i++) {
        for (int j = 0; j < n; j++) {
            if (adj[i][j]) inDegree[j]++;
        }
    }
    
    int queue[MAX], front = 0, rear = 0;
    for (int i = 0; i < n; i++) {
        if (inDegree[i] == 0) queue[rear++] = i;
    }
    
    while (front < rear) {
        int v = queue[front++];
        printf("%d ", v);
        for (int i = 0; i < n; i++) {
            if (adj[v][i] && --inDegree[i] == 0) {
                queue[rear++] = i;
            }
        }
    }
}`},{key:"stack",category:"linear",name:"栈",desc:"后进先出（LIFO）的线性结构，支持 Push 和 Pop 操作。",component:"StackVisualizer",difficulty:"简单",time:"O(1)",space:"O(n)",stable:"-",steps:["Push：将新元素放到栈顶","Pop：移除并返回栈顶元素","仅在一端进行操作"],pseudo:`push(x): stack[top++] = x
pop(): return stack[--top]`,code:`#define MAX 100
int stack[MAX];
int top = -1;

void push(int x) {
    if (top < MAX-1) stack[++top] = x;
}

int pop() {
    if (top >= 0) return stack[top--];
    return -1;
}`},{key:"queue",category:"linear",name:"队列",desc:"先进先出（FIFO）的线性结构，支持 Enqueue 和 Dequeue。",component:"QueueVisualizer",difficulty:"简单",time:"O(1)",space:"O(n)",stable:"-",steps:["Enqueue：在队尾加入元素","Dequeue：从队头移除元素","两端分别操作，保持 FIFO"],pseudo:`enqueue(x): queue[rear++] = x
dequeue(): return queue[front++]`,code:`#define MAX 100
int queue[MAX];
int front = 0, rear = 0;

void enqueue(int x) {
    if (rear < MAX) queue[rear++] = x;
}

int dequeue() {
    if (front < rear) return queue[front++];
    return -1;
}`},{key:"linked-list",category:"linear",name:"链表",desc:"通过指针链接节点的线性结构，支持插入、删除与反转。",component:"LinkedListVisualizer",difficulty:"中等",time:"O(n)",space:"O(n)",stable:"-",steps:["创建节点并保存数据域与指针域","插入：修改前驱节点的 next 指向新节点","删除：修改前驱节点的 next 跳过后继节点","反转：逐个将当前节点 next 指向前驱"],pseudo:`insert(p, x):
    node = new Node(x)
    node.next = p.next
    p.next = node`,code:`typedef struct Node {
    int val;
    struct Node *next;
} Node;

Node* insert(Node* head, int val) {
    Node* node = (Node*)malloc(sizeof(Node));
    node->val = val;
    node->next = head;
    return node;
}

Node* reverse(Node* head) {
    Node *prev = NULL, *cur = head, *next;
    while (cur) {
        next = cur->next;
        cur->next = prev;
        prev = cur;
        cur = next;
    }
    return prev;
}`}];function o(i,t){return r.find(e=>e.category===i&&e.key===t)}export{r as a,n as c,o as f};
