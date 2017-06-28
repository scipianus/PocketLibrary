% Parameters
params.numeDirector = './m_covers_0000/';
params.tipImagine = 'jpg';
params.imagineQuery = 'query8.jpg';
params.directorQuery = './test_set/';
params.numarMaximImagini = 10000;
params.numeFisierDB = 'scores_m';
params.numeFisierText = 'scores_m_matlab.txt';
params.topQuery = 5;

% Setup MatConvNet.
run vl_setupnn ;

% Load a model and upgrade it to MatConvNet current version.
net = load('imagenet-caffe-alex.mat') ;
net.layers = net.layers(1:18);
net = vl_simplenn_tidy(net) ;

filelist = dir([params.numeDirector '*.' params.tipImagine]);
nrImages = min(params.numarMaximImagini, length(filelist));
scores = zeros(nrImages, 4096);

try
    tmp = load(params.numeFisierDB);
    scores = tmp.scores;
    disp('Am incarcat descriptorii pentru pozele din baza de date');
catch
    disp('Construim descriptorii pentru pozele din baza de date:');
    
    fprintf('Incarcam pozele din director \n');
    for idxImg = 1:nrImages
        disp(['Incarcam poza ' num2str(idxImg) '/' num2str(nrImages)]);    

        % Obtain and preprocess an image.
        imgName = [params.numeDirector filelist(idxImg).name];
        im = imread(imgName) ;
        im_ = single(im) ; % note: 255 range
        im_ = imresize(im_, net.meta.normalization.imageSize(1:2)) ;
        im_ = im_ - net.meta.normalization.averageImage ;

        % Run the CNN.
        res = vl_simplenn(net, im_) ;

        % Gather scores
        scores(idxImg,:) = squeeze(gather(res(end).x));
    end
    save(params.numeFisierDB,'scores');
    dlmwrite(params.numeFisierText, scores);
    disp(['Am salvat descriptorii pentru pozele din baza de date in fisierul ' params.numeFisierDB]);
end
filelistQuery = dir([params.directorQuery '*.' params.tipImagine]);
f = figure('units','normalized','outerposition',[0 0 1 1]) ;
for i = 1:size(filelistQuery)
    % Obtain and preprocess an image.
    imgName = [params.directorQuery filelistQuery(i).name];
    im = imread(imgName);
    clf;
    subplot(1,params.topQuery + 1,1); imshow(im);
    title('Imaginea cautata');
    im_ = single(im) ; % note: 255 range
    im_ = imresize(im_, net.meta.normalization.imageSize(1:2));
    im_ = im_ - net.meta.normalization.averageImage;

    % Run the CNN.
    res = vl_simplenn(net, im_);

    % Gather scores
    scoresQuery = squeeze(gather(res(end).x));
    scoresQuery = repmat(scoresQuery', [size(scores,1) 1]);

    similarities = sum(abs(scores - scoresQuery), 2);
    [minDist, idxMinDist] = sort(similarities);

    for idx = 1:params.topQuery
        imgName = [params.numeDirector filelist(idxMinDist(idx)).name];
        im = imread(imgName);
        subplot(1,params.topQuery + 1,idx + 1); imshow(im);
        title(sprintf('Imaginea gasita #%d\n %s, score %.3f', idx, filelist(idxMinDist(idx)).name, minDist(idx)));
    end 
    %saveas(f,['query_result' num2str(i) '.jpg']);
    waitforbuttonpress;
end


